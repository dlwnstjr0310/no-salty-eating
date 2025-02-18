package com.study.payment.application.service

import com.study.payment.application.dto.event.consumer.CreateOrderEvent
import com.study.payment.application.dto.event.provider.PaymentResultEvent
import com.study.payment.application.exception.NotFoundPaymentException
import com.study.payment.domain.model.Payment
import com.study.payment.domain.model.PgStatus.AUTH_SUCCESS
import com.study.payment.domain.model.PgStatus.CAPTURE_REQUEST
import com.study.payment.domain.model.PgStatus.CAPTURE_SUCCESS
import com.study.payment.domain.repository.PaymentRepository
import com.study.payment.infrastructure.messaging.provider.KafkaMessagePublisher
import kotlinx.coroutines.delay
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Service
class PaymentTestService(
    private val cacheService: CacheService,
    private val kafkaProducer: KafkaMessagePublisher,
    private val paymentRepository: PaymentRepository,
) {

    companion object {
        private const val PAYMENT_RESULT = "payment-result"
    }

    @Transactional
    suspend fun createPaymentInfoTest(createOrderEvent: CreateOrderEvent) {
        val payment = paymentRepository.save(
            Payment(
                userId = createOrderEvent.userId,
                description = createOrderEvent.description,
                paymentPrice = createOrderEvent.paymentPrice,
                pgOrderId = createOrderEvent.pgOrderId,
            )
        )

        keyInjection(payment.id)
    }

    suspend fun keyInjection(paymentId: Long) {

        val payment = getPayment(paymentId).apply {
            this.injectionPgKey(makePgKey())
            this.updateStatus(AUTH_SUCCESS)
            this.increaseRetryCount()
        }

        cacheService.put(payment.id)
        payment.updateStatus(CAPTURE_REQUEST)

        payment.updateStatus(CAPTURE_SUCCESS)
        paymentRepository.save(payment)
        cacheService.remove(payment.id)

        kafkaProducer.sendEvent(
            PAYMENT_RESULT, PaymentResultEvent(
                payment.pgOrderId!!,
                payment.paymentPrice,
                payment.pgStatus.name,
                payment.description!!,
            )
        )
    }

    @Transactional
    suspend fun retryRequestPayment(paymentId: Long) {
        getPayment(paymentId).let {
            delay(getDelay(it))
            keyInjection(paymentId)
        }
    }

    private suspend fun getPayment(paymentId: Long): Payment {
        return paymentRepository.findById(paymentId) ?: throw NotFoundPaymentException()
    }

    private fun makePgKey(): String {
        val chars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        val randomString = (1..5)
            .map { chars.random() }
            .joinToString("")

        return "tgen_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}$randomString"
    }

    private fun getDelay(payment: Payment): Duration {
        val temp = (2.0).pow(payment.pgRetryCount).toInt() * 1000
        val delay = temp + (0..temp).random()
        return delay.milliseconds
    }
}
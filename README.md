# QUICK-COMMERCE
![퀵커머스로고](https://github.com/user-attachments/assets/b1fd75bb-3038-4337-a9e8-3c8445d15e70)

📃주문-결제 관련 내용만 다루고 있습니다. [프로젝트 브로셔](https://www.notion.so/teamsparta/3-Quick-Commerce-fa5f015f37c04514a04695c1ee8833f2)

📃배포 URL (현재 중단) : http://43.200.183.151:9090/eureka/apps


## 특징

Webflux / Coroutine 을 사용하여 최소한의 서버 비용으로 최대한의 성능을 뽑아내기 위한 reactive 서비스 <br> <br>
개발기간 : 2024.12.26 ~ 2024.01.27 <br>

- 고려한 점
  - 주문 - 결제 시스템은 사용자와 가장 근접한 위치에서 작동
  - 빠른 피드백과 시스템의 안정성이 매우 중요함
  - MSA 환경에서의 비용 효율성 <br>
-> Thread per Request 모델이 아닌, 적은 리소스로 높은 효율을 낼 수 있는 Webflux 사용
  - JDBC 에서 비동기적으로 DB에 데이터를 저장하는 경우, 어플리케이션 레벨의 캐시를 사용하므로 안정성이 굉장히 낮음

## 인프라 구성도
![Image](https://github.com/user-attachments/assets/9aae28c4-6cb0-4dea-a3ec-1708f000a902)

## 개발환경 
- **Language :** Kotlin 1.9.25
- **Build Tool :** Gradle
- **Framework :** Spring boot 3.4.1
- **DB :** MariaDB, Redis
- **Deploy :** AWS ECR, AWS ECS, Docker, Github Action
- **Library :** Spring Webflux, Spring R2DBC, Coroutine, WebClient, ElasticSearch, Kibana

<br>

## 기술적 의사결정

- `Coroutine`
  - Java 21의 버추얼 스레드와 JDBC 를 사용하는 경우, Pinning 발생 문제 인지
  - 함수형 프로그래밍의 러닝 커브 완화
- `R2DBC`
  - 비동기 방식으로 RDBMS 사용 시 데이터 일관성 보장
- `Redis : Lua Script`
  - 분산 락 사용 시 락 획득 스레드와 해제 스레드가 다른 경우 락 해제 불가
    - 모든 스레드가 작업중인 경우, 데드락 발생
  - LeaseTime 과 finally 블록을 사용하여 락 해제 보장
    - 처리량이 떨어져 Kafka 에서 Consumer rebalance 문제 발생
  - 락 대신 LuaScript 를 이용하여 원자성 보장

## 주요 기능
- 주문, 결제
  - Webflux, Coroutine 기반의 비동기 서비스 구축
  - 스레드 간 request 공유를 위해 MdcContext를 활용하여 Reactor Context 사용
  - 서버 셧다운으로 인한 데이터 유실을 방어하기 위해 Redis 를 이용하여 주문,결제 데이터 관리
  - 결제 실패 시 지수 Backoff 와 Jitter 를 이용한 규칙성 없는 재시도
  - Kafka 의 Idempotence 속성을 이용한 결제 중복 처리 방지
  - Elastic Search 를 사용하여 조회 성능 개선 및 서비스 안정성 개선  
- 인프라 구축
  - GitHub Actions를 활용한 CI/CD 구성
  - AWS ECR 과 ECS 를 활용한 서비스 배포

## 트러블 슈팅

### Issue #1
**Kafka Consumer Rebalancing**

- 원인
  1. 주문 생성에 트래픽이 몰린 경우, 요청처리에 모든 리소스를 사용하여 heartbeat 만료
  2. 대체할 컨슈머가 없기 때문에, offset 정보와 그룹 정보 보존을 위하여 empty consumer 등록
  <br>
- 해결 과정
  - 총 1만명의 각각 다른 사용자, 상품, 주문 갯수 모두 무작위 <br>

    <details>
    <summary>CPU : 0.5vCpu / RAM : 1GB ( < t2.micro ) </summary>
    
      ![image.png](https://github.com/user-attachments/assets/06b2bbbc-4491-416d-9f49-30179ebc4876) <br>
      - 62ms 의 응답속도와 CPU 사용률을 근거로, 로직이 아닌 서버 자원의 문제라 판단

    </details>

    <details>
    <summary>CPU : 1vCpu / RAM : 2GB ( = t2.small ) </summary>
    
      ![Image](https://github.com/user-attachments/assets/1b3c4367-deaa-43fd-9982-fe14c3f09706) <br>
      - 평균 응답속도는 56%, TPS 는 249% 증가 <br>
      - 여전히 테스트마다 CPU 사용률은 100% 기록, heartbeat 와 session timeout 설정을 조정했음에도 여전히 리밸런싱 문제 발생
        <details>
         <summary>CPU / MEM 사용률 </summary>
         
         ![Image](https://github.com/user-attachments/assets/3a176a39-dd37-41d8-b5be-a4ae0f5fd504)
    
        </details>


    </details>

    <details>
    <summary>CPU : 2vCpu / RAM : 4GB ( = t2.medium ) </summary>
    
      ![Image](https://github.com/user-attachments/assets/17108c21-4ce9-44ea-a77d-aaa6d83d748d) <br>
      - 평균 응답속도는 33%, 95% 라인은 50%, 99%라인은 약 60% 개선 <br>
      - CPU 사용량에 여유가 생긴만큼 consumer rebalancing 문제 해결
        <details>
         <summary>CPU / MEM 사용률 </summary>
         
         ![Image](https://github.com/user-attachments/assets/bdd77a32-06f5-45e1-b8b1-e05d63f44208)
    
        </details>

    </details>

### Issue #2
**Connection Pool 조정**

- 원인
  - 평균 처리 속도와 CPU 사용량을 근거로, DB 커넥션이 모자라 요청을 처리하지 못하는것이라 판단
    <details><summary>기본 커넥션 풀 사용 시 가장 효율적인 처리량</summary>
     
       ![Image](https://github.com/user-attachments/assets/23b7ead5-d4ee-4368-aad9-b4166c437e02)

       ![Image](https://github.com/user-attachments/assets/f2502df7-0268-4c54-878b-18e9a61b63aa)
       - 기본 커넥션 풀 사용 시, ramp-up 을 16으로 설정했을 때 32%의 CPU 사용량과 평균 23ms의 응답속도를 확인할 수 있음
    </details>
    
 - 해결 과정
   1. YML 파일에서 커넥션 풀 설정 변경시 성능 저하
      <br>
      - YML 파일에서 커넥션 풀 설정 시, 사용하게 되는 구현체는 **springframwork.boot.autoconfigure.r2dbc**
      - 기존 URL 방식으로 사용한 커넥션 풀 구현체는 **io.r2dbc.pool**로, 커넥션 풀 구현체가 변경되어 성능 저하
      - 기존 방식을 그대로 사용하거나, Config 파일로 직접 커넥션 풀 구현체 생성해야함
      
   2. Config 파일로 커넥션 풀 설정 변경시 성능 저하
      <br>
      - io.r2dbc.pool 구현체의 기본 설정으로 colocation 기능이 활성화 되어있음
      - colocation 기능으로 인해 버추얼 스레드의 pinning 이슈와 유사하게 특정 커넥션의 쿼리가 지정된 스레드에서만 처리하도록 설정됨
      - LoopResources 를 직접 설정하여 colocation 비활성화 하여 해결
      - 이후 커넥션 풀 조정 시 반드시 성능이 저하됨
        
  - 결론
    - 커넥션 풀 커스텀시 사이드 이펙트를 확인하지 못하고 있는 상태
    - 추후 커넥션 풀 조정에 성공했을 때, 큰 폭의 성능 개선이 이루어지지 않을까 짐작됨
   

## 개선점
- 트랜잭셔널 아웃박스 패턴을 도입하여 DB 업데이트와 메시지 발행을 하나의 트랜잭션으로 처리
- 스케일 업이 아닌 스케일 아웃을 통해 문제를 해결할 순 없었을까?
- Redis 의존성이 높기때문에, master - slave 구조 및 sentinel 활용
- spring multi profile 을 통해 local - test - dev 환경 분리

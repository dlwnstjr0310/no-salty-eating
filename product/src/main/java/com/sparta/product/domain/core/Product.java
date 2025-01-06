package com.sparta.product.domain.core;

import com.sparta.product.application.dtos.product.ProductRequestDto;
import com.sparta.product.application.dtos.product.ProductUpdateRequestDto;
import com.sparta.product.application.exception.category.NotFoundCategoryException;
import com.sparta.product.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Getter
@DynamicUpdate
@Table(name = "TB_PRODUCT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Integer price;
    private Integer stock;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductCategory> productCategoryList = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeSaleProduct> timeSaleProductList = new ArrayList<>();

    public void addProductCategoryList(ProductCategory productCategory) {
        productCategoryList.add(productCategory);
        productCategory.updateProduct(this);
    }

    public void addTimeSaleProductList(TimeSaleProduct timeSaleProduct) {
        timeSaleProductList.add(timeSaleProduct);
        timeSaleProduct.updateProduct(this);
    }

    public static Product createFrom(ProductRequestDto productRequestDto) {
        return new Product(
                productRequestDto.productName(),
                productRequestDto.price(),
                productRequestDto.stock(),
                new ArrayList<>(),
                productRequestDto.isPublic()
        );
    }

    private Product(String name, Integer price, Integer stock, List<ProductCategory> productCategoryList, Boolean isPublic) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.productCategoryList = productCategoryList;
        super.updateIsPublic(isPublic);
    }

    public void updateFrom(ProductUpdateRequestDto productUpdateRequestDto) {
        if (productUpdateRequestDto.productName() != null) {
            name = productUpdateRequestDto.productName();
        }

        if (productUpdateRequestDto.price() != null) {
            price = productUpdateRequestDto.price();
        }

        if (productUpdateRequestDto.stock() != null) {
            stock = productUpdateRequestDto.stock();
        }

        if (productUpdateRequestDto.isPublic() != null) {
            super.updateIsPublic(productUpdateRequestDto.isPublic());
        }
    }

    public void updateCategories(List<Long> categoryIds, Map<Long, Category> categoryMap) {
        this.productCategoryList.clear();

        for (Long categoryId : categoryIds) {
            Category findCategory = categoryMap.get(categoryId);
            if (findCategory == null) {
                throw new NotFoundCategoryException();
            }
            ProductCategory productCategory = ProductCategory.createOf(this, findCategory);
            this.addProductCategoryList(productCategory);
        }
    }
}

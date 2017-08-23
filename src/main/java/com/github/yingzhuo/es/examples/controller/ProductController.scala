package com.github.yingzhuo.es.examples.controller

import com.github.yingzhuo.es.examples.service.ProductService
import com.typesafe.scalalogging.LazyLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation._

@RestController
@RequestMapping(Array("/products"))
class ProductController @Autowired()(val productService: ProductService) extends LazyLogging {

    @GetMapping(Array("/{id}/"))
    def findOne(@PathVariable("id") id: String): Json = {
        logger.debug("通过ID({})查找产品", id)
        val prod = productService.findProductById(id)
        Json("product" -> prod)
    }

    @PutMapping(Array("/{id}/name/{name}/"))
    def changeName(@PathVariable("id") id: String, @PathVariable("name") name: String): Json = {
        logger.debug("通过ID({})修改产品名称({})", id, name)
        val prod = productService.changeProductName(id, name)
        Json("product" -> prod)
    }

    @PutMapping(Array("/{id}/price/{price}/"))
    def changePrice(@PathVariable("id") id: String, @PathVariable("price") price: Double): Json = {
        logger.debug("通过ID({})修改产品价格({})", id, price)
        val prod = productService.changeProductPrice(id, price)
        Json("product" -> prod)
    }

    @PutMapping(Array("/{id}/description/{desc}"))
    def changeDescription(@PathVariable("id") id: String, @PathVariable("desc") description: String): Json = {
        logger.debug("通过ID({})修改产品详情({})", id, description)
        val prod = productService.changeProductDescription(id, description)
        Json("product" -> prod)
    }

}
package com.kopa.controller;

import com.kopa.service.ProductService;
import com.kopa.service.TableService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    private final ProductService productService;
    private final TableService tableService;

    public WebController(ProductService productService, TableService tableService) {
        this.productService = productService;
        this.tableService = tableService;
    }

    @GetMapping({"/", "/index", "/home"})
    public String index(Model model) {
        model.addAttribute("appName", "KOPA Coffee Roasters");
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("tables", tableService.getAllTables());
        return "index";
    }

    @GetMapping({"/signin", "/login"})
    public String signin(Model model) {
        model.addAttribute("appName", "KOPA Coffee Roasters");
        return "signin";
    }

    @GetMapping({"/signup", "/register"})
    public String signup(Model model) {
        model.addAttribute("appName", "KOPA Coffee Roasters");
        return "signup";
    }

    @GetMapping("/404")
    public String notFound(Model model) {
        model.addAttribute("path", "/404");
        return "error/404";
    }
}

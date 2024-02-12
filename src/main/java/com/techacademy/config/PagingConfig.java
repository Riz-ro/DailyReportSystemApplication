package com.techacademy.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 全てのコントローラーに対して共通処理を行う
@Configuration
public class PagingConfig implements WebMvcConfigurer {

    // 設定を補完する情報のことをリゾルバ(resolver)と呼ぶ
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {

        // Pageableに対して設定を行うためのクラスであり、リゾルバ
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();

        // ページ単位に表示する件数を追加(第一引数：ページ番号、第二引数：1ページあたりの表示件数)
        resolver.setFallbackPageable(PageRequest.of(0, 8));

        // 具体的な設定をリゾルバに追加後、リストに追加
        argumentResolvers.add(resolver);
    }
}

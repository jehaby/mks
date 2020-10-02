Приложение для пункта выдачи заказов.

[Исходное ТЗ](https://docs.google.com/document/d/1xqsWxsj-Jce0fKbazC7S0uiY6Ze4zFkCx6r9sNfzpes/edit)

## Техническая часть 

SPA на Clojurescript. Использует библиотеку `reagent`, которая представляет из себя обвязку над реактом.

Готовый javascript компилируется из `*.cljs` с помощью утилиты [shadow-cljs](https://shadow-cljs.github.io/docs/UsersGuide.html#_standalone_via_code_npm_code): 

    npm install

    npx shadow-cljs release :app
    

Приложение общается с МКС через API. Пользователь должен быть авторизован в МКС.

### Запуск тестов 

    TODO

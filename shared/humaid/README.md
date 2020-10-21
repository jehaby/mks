Приложение для пункта выдачи заказов.

[Исходное ТЗ](https://docs.google.com/document/d/1xqsWxsj-Jce0fKbazC7S0uiY6Ze4zFkCx6r9sNfzpes/edit)

## Техническая часть 

SPA написанное на ClojureScript. Использует библиотеку [Reagent](https://reagent-project.github.io/), которая представляет из себя обвязку над [реактом](https://reactjs.org/).

В качестве сборщика используется утилита [shadow-cljs](https://shadow-cljs.github.io/docs/UsersGuide.html#_standalone_via_code_npm_code) (что-то вроде аналога webpack/gulp для ClojureScript).

Приложение общается с МКС через API. Пользователь должен быть авторизован в МКС.

## Сборка для продакшена

Готовая статика (html/css/js) собирается с помощью докер-образа (`/docker/cljs/Dockerfile`) командой

    make humaid_build_prod # из корневой директории mks
    # под капотом: docker build  --file ./docker/cljs/Dockerfile --tag mks_humaid_app ./shared/humaid/

Затем образ `mks_humaid_app` используется при сборке образа ``nginx`` (см. `/docker/nginx/Dockerfile`).
Чтобы увидеть сделанные в коде изменения нужно пересобрать и перезапустить сервирс `nginx`.

    docker-compose up --build nginx

## Разбработка

Для разработки рекомендуется установить локально [shadow-cljs](https://shadow-cljs.github.io/docs/UsersGuide.html#_installation).

Затем в директории проекта (`./shared/humaid`):

    npm install

    npx shadow-cljs watch :app

Приложение будет доступно по адресу http://localhost:8142. Порт можно поменять в конфиге (`shadow-cljs.edn`).

Среди кложуристов принято запускать это дело из редактора/IDE. Если интересно узнать больше то можно погуглить "clojure[script] repl driven development". Например [[1](https://www.youtube.com/watch?v=rQ802kSaip4)], [[2](https://www.youtube.com/watch?v=Qx0-pViyIDU)].


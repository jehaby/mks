# Образ для сборки приложения пунтка выдачи (./shared/humaid).

FROM theasp/clojurescript-nodejs:shadow-cljs AS build

WORKDIR /humaid

COPY shadow-cljs.edn package.json package-lock.json ./
COPY src src
COPY public/index.html public/index.html
COPY public/css public/css

RUN shadow-cljs npm-deps
RUN npm install --save-dev shadow-cljs
RUN shadow-cljs release :app

CMD ["sh"]

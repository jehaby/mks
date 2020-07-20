(ns humaid.app
  (:require
   [ajax.core :refer [GET POST]]
   [clojure.string :as string]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [goog.string :as gstring]
   [goog.string.format]
   [humaid.notification :as ntfc]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [reitit.core :as reitit]
   [reitit.frontend :as rf])
  (:import goog.History))


(def allowed-time 120)
(defonce timer-fn (atom nil))

(defonce session (r/atom {:page :home
                          :ready true
                          :timer-started false
                          :timer-val allowed-time
                          }))

(defn timer [sec]
  (let [min  (quot sec 60)
        sec  (rem sec 60)]
    [:div.is-size-2 (gstring/format "%02d:%02d" min sec)]))

(defn reset-timer! []
  (js/clearInterval @timer-fn)
  (swap! session assoc :timer-val allowed-time
         :timer-started false))

(defn start-timer! []
  (when-not (:timer-started @session)
    (swap! session assoc :timer-started true)
    (reset! timer-fn
            (js/setInterval
             (fn [] (swap! session update :timer-val
                           #(if (< 0 %) (dec %) %)))
             1000))))

(defn set-hash! [loc]
  (set! (.-hash js/window.location) loc))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page (:page @session)) "is-active")}
   title])

(defn navbar []
  (r/with-let [expanded? (r/atom false)]
    [:nav.navbar.is-info>div.container
     [:div.navbar-brand
      [:a.navbar-item {:href "/" :style {:font-weight :bold}} "humaid"]
      [:span.navbar-burger.burger
       {:data-target :nav-menu
        :on-click #(swap! expanded? not)
        :class (when @expanded? :is-active)}
       [:span][:span][:span]]]
     [:div#nav-menu.navbar-menu
      {:class (when @expanded? :is-active)}
      [:div.navbar-start
       [nav-link "#/" "Home" :home]
       [nav-link "#/about" "About" :about]]]]))

(def clients [
              {:name "Григорий Свердлин"
               :id 1
               :photo "https://homeless.ru/upload/resize_cache/iblock/5d8/632_420_2/954c51d4c8d51fa043c97d74e508bfab.jpg"
               :date-of-bearth "1234-01-02"
               }
              {:name "Андрей Чапаев"
               :id 2
               :photo "https://homeless.ru/upload/resize_cache/iblock/952/632_420_2/964c4ec5359e4eb47af6539a10104639.jpg"
               :date-of-bearth "1234-01-02"
               }
              {:name "Данил Краморов"
               :id 3
               :photo "https://homeless.ru/upload/resize_cache/iblock/c64/632_420_2/_.jpg"
               :date-of-bearth "1234-01-02"
               }
              {:name "Карина Гаринова"
               :id 4
               :photo "https://homeless.ru/upload/resize_cache/iblock/fc1/632_420_2/b0fa7c4a83c5216c4dcfe793d6331f3b.jpg"
               :date-of-bearth "1234-01-02"
               }
              {:name "Валентина Марьяновна Борейко"
               :id 5
               :photo "https://homeless.ru/upload/resize_cache/iblock/518/632_420_2/33841d18341c3c6f3a952e40c070622a.jpg"
               :date-of-bearth "1234-01-02"
               }
              {:name "Василиса Молева"
               :id 6
               :photo "https://homeless.ru/upload/resize_cache/iblock/760/632_420_2/Vasilisa_sm.jpg"
               :date-of-bearth "1234-01-02"
               }
              {:name "Дарья Лысухина"
               :id 7
               :photo "https://homeless.ru/upload/iblock/ff0/dasha_lysukhina.jpg"
               :date-of-bearth "1234-01-02"
               }
              {:name "Ксения Солодова"
               :id 8
               :photo "https://homeless.ru/upload/resize_cache/iblock/93c/632_420_2/TptfwYiRgcQ.jpg"
               :date-of-bearth "1234-01-02"
               }
              ])

(defn client-search [name]
  (if (empty? name)
    []
    (filter #(re-matches (re-pattern (str "(?i).*" name ".*")) (:name %)) clients)))

(defn handle-search [state event]
  ;;  (start-timer!)
  (let [name (-> event .-target .-value)]
    (if (empty? name)
      (do (reset-timer!)
          (swap! state assoc :search-res nil))
      (when (> (count name) 2)
        (do (start-timer!)
            (when-let [res (client-search name)]
              (swap! state assoc :search-res res)))))))

(defn start-page []
  (r/with-let [state (r/atom {} )
               _   (reset-timer!)]
    [:section.section>div.container>div.content
     [:section.columns
      [:div.column.is-2
       [timer (:timer-val @session)]]

      [:div.column
       [:h3 "Поиск клиента"]
       [:div.control
        [:input.input.is-large
         {:type "text"
          :placeholder "ФИО..."
          :on-change #(handle-search state %)
          }]]

       (when-let [clients (:search-res @state)]
         (for [{:keys [id name date-of-bearth]} clients]
           [:div.is-size-2
            [:a {:href (str "/#/clients/" id) :key id}
             (str name " (" date-of-bearth ")")]] ;; TODO styling
           ))]]]))

(defn delivery-link [id kind]
  (str "/#/clients/" id "/delivery/" (name kind)))

;; спортивные штаны, трико, юбки, пиджак, кофта, толстовка, свитер, бадлон, рубашки, куртка, ремень, кепка, шапка, перчатки
(defn load-client! [id]
  (if-let [client (-> (filter #(= id (str (:id %))) clients) first)]
    (swap! session assoc :client
           (merge client
                  {:services
                   {:clothes
                    {:shoes {:title "Обувь"}
                     :underwear {:title "Нижнее белье"}
                     :socks {:title "Носки"}
                     :t-shirts {:title "Футболки" :unavailable-until "2020-07-15"}
                     :jeans {:title "Джинсы" :unavailable-until "2020-07-18"}
                     :trousers {:title "Штаны"}
                     :sweatpans {:title "Спортивные штаны"}
                     :tights {:title "Трико"}
                     :skirts {:title "Юбки"}
                     :blazer {:title "Пиджак"}
                     :blouse {:title "Кофта"}
                     :hoody  {:title "Толстовка"}
                     :sweater {:title "Свитер"}
                     :badlon {:title "Бадлон"}
                     :shirts {:title "Рубашки"}
                     :jacket {:title "Куртка"}
                     :belt {:title "Ремень"}
                     :cap {:title "Кепка"}
                     :hat {:title "Шапка"}
                     :gloves {:title "Перчатки"}
                     }
                    :hygiene
                    {:shaver {:title "бритвенный станок"}
                     :soap {:title "мыло"}
                     :toothbrush {:title "щетка"}
                     :toothpaste {:title "паста"}
                     :cream {:title "крем"}
                     :pantyliners {:title "прокладки"}
                     :napkins {:title "салфетки"}
                     :comb {:title "расческа"}
                     :nail-clippers {:title "кусачки для ногтей"  :unavailable-until "2020-07-14"}
                     :shampoo {:title "шампунь"}
                     :deodorant {:title "дезодорант"}
                     :shower-gel {:title "гель для душа"}
                     :cotton-buds {:title "ватные палочки"}
                     }
                    :crutches
                    {:crutches {:title "Костыли/трость"}}
                    }
                   }
                  )
           )
    (swap! session dissoc :client)))

(defn client-page [{{id :id} :path}]
  (load-client! id)
  (start-timer!)
  (let [client (:client @session)]
    [:section.section.container.content
     [:section.columns
      [:div.column.is-2
       [timer (:timer-val @session)]
       [:button.button {:on-click #(set-hash! "/")} "Завершить выдачу"]]

      [:div.column.is-3
       [:p>a {:href (delivery-link (:id client) :clothes)}
        [:button.button.is-large.is-block "Одежда"]]
       [:p>a {:href (delivery-link (:id client) :hygiene)}
        [:button.button.is-large.is-block "Гигиена"]]
       [:p>a {:href (delivery-link (:id client) :crutches)}
        [:button.button.is-large.is-block "Костыли и трости"]]]

      [:div.column.is-3
       [:img
        {:src (:photo client)}]
       [:p.is-size-5 (:name client)]
       ]]
     ]))

(defn redirect-modal [state]
  [:div.modal {:class [(when (:active? @state) "is-active")]} ;; TODO modal formatting
   [:div.modal-background]
   [:div.modal-card
    [:div.modal-card-head]
    [:div.modal-card-body
     [:div.content "Есть несохранённые изменения. Продолжить?"]]
    [:div.modal-card-foot
     [:button.button {:on-click #(set-hash! (:url @state))} "Да"]
     [:button.button {:on-click #(swap! state assoc :active? false) :aria-label "close"} "Нет"]]
    ]])

(defn delivery-page [{{kind :kind id :id} :path}]  ;; TODO: what if client-id and session client differs?
  (when-not (:client @session)
    (load-client! id)) ;; TODO: should I do this?
  (r/with-let [kind (keyword kind)
               heading (str "Выдача " (kind {:clothes "одежды"
                                             :hygiene "предметов гигиены"
                                             :crutches "костылей и тростей"}))
               selected-items (r/atom #{})
               switch-selected! (fn [selected-items item-key]
                                  (if (contains? @selected-items item-key)
                                    (swap! selected-items disj item-key)
                                    (swap! selected-items conj item-key)))

               save! (fn [selected-items kind]
                       (doseq [i @selected-items]
                         (swap! session assoc-in [:client :services kind i :unavailable-until] "2020-08-30"))
                       (reset! selected-items #{})
                       (ntfc/show! "success" "Выдача сохранена."))

               redirect-modal-state (r/atom {:active? false :url nil})
               redirect! (fn [url selected-items]
                           (if (empty? @selected-items)
                             (set-hash! url)
                             (swap! redirect-modal-state assoc :active? true
                                    :url url)
                             ))]

    [:section.section.container.content

     [redirect-modal redirect-modal-state]

     [:section.columns
      [:div.column.is-2
       [timer (:timer-val @session)]
       [:p>button.button
        {:on-click #(redirect! (str "/clients/" id) selected-items)}
        "Вернуться к списку"]
       [:p>button.button.is-light.is-danger
        {:on-click #(redirect! "/" selected-items)}
        "Завершить выдачу"]]

      [:section.column
       (let [selected @selected-items
             client (:client @session)
             items (kind (:services client))]
         [:div.container.content
          [:h3 heading]
          [:p.is-size-5.has-text-weight-light (str " " (:name client))]
          [:section
           (for [[item-key {title :title unavailable-until :unavailable-until}] items]
             (if unavailable-until
               [:button.button.mx-2.my-2.is-size-5
                {:disabled true
                 :key item-key
                 :dangerouslySetInnerHTML {:__html (str (clojure.string/capitalize title) (str "<br> (доступно с " unavailable-until ")"))}
                 }]
               [:button
                {:key item-key
                 :class ["button" "is-large" "mx-2" "my-2"
                         (when (contains? selected item-key) "is-success")]
                 :on-click #(switch-selected! selected-items item-key)}
                (clojure.string/capitalize title)]))]
          ])
       [:div.container.content
        [:button.button.is-large.is-light.is-success
         {:disabled (empty? @selected-items)
          :on-click #(save! selected-items kind)}
         "Сохранить"]]]]]))

(def pages
  {:start #'start-page
   :client #'client-page
   :delivery #'delivery-page})

(defn not-found-page []
  [:section.section>div.container>div.content
   [:p "404 (страница не найдена)"]
   [:p  [:a {:href "#/"} "назад"]]])

(defn modal-loading []
  [:div.modal.is-active
   [:div.modal-backgroud]
   [:div.modal-content
    [:div.content "Загружается..."]
    [:button.button.is-large.is-loading " -----------"]
    ]])

(defn page []
  (let [s @session]
    (if (:ready s)
      [(pages (:page s) #'not-found-page) (:parameters s)]
      [modal-loading]
      )))

;; -------------------------
;; Routes

(def router
  (rf/router
   [["/" :start]
    ["/search" :search]
    ["/clients/:id" :client]
    ["/clients/:id/delivery/:kind" :delivery]]
   ;;   {:data {:coercion rss/coercion}}
   ))

(defn match-route [uri]
  (->> (or (not-empty (string/replace uri #"^.*#" "")) "/")
       (rf/match-by-path router)))
;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     HistoryEventType/NAVIGATE
     (fn [event]
       (let [m (match-route (.-token event))]
         (swap! session assoc
                :page (get-in m [:data :name])
                :parameters (:parameters m)))))
    (.setEnabled true)))

;; -------------------------

(defn mount-components []
  ;;  (rdom/render [#'navbar] (.getElementById js/document "navbar"))
;;  (rdom/render [#'ntfc/notification] (.getElementById js/document "notification"))
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
  ;;  (ajax/load-interceptors!)

  (prn "in init! ")

  (hook-browser-navigation!)
  (mount-components))

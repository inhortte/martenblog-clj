(ns martenblog.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [ajax.core :refer [ajax-request url-request-format json-response-format]]))

;; -------------------------
;; Views

(def entries-per-page 11)

(def ^:private lepton (atom {:entry-count 40
                             :topic {:topics []
                                     :filter ""}}))

(defn- ajax-req-sin-params
  "Returns a map to be used with ajax-request
  uri is obvious
  asociar is an array for assoc-in (to place the result in the lepton atom)
  in-key is the key to utilize from the request map"
  [uri asociar in-key]
  (do
    (js/console.log (str "uri: " uri "  asociar: " asociar "  in-key: " in-key))
    {:uri uri
     :method :post
     :format (url-request-format)
     :response-format (json-response-format {:keywords? true})
     :handler (fn [[ok res]]
                (if ok
                  (swap! lepton assoc-in asociar (in-key res))
                  (do
                    (js/console.log (str "Death to you, VOLE: " (:status-text res))))))}))

(defn- get-entry-count []
  (ajax-request (ajax-req-sin-params "/paginas" [:entry-count] :entryCount)))

(defn- page-links []
  (let [page-count (+ (quot (:entry-count @lepton) entries-per-page)
                      (if (zero? (rem (:entry-count @lepton) entries-per-page)) 0 1))
        the-links (for [p (range 1 (inc page-count))] [:a {:href (str "/entries/page/" p)} p])]
    (get-entry-count)
    [:div the-links]))


(defn- get-topics []
  (ajax-request {:uri "/topics"
                 :method :post
                 :format (url-request-format)
                 :response-format (json-response-format {:keywords? true})
                 :handler (fn [[ok res]]
                            (if ok
                              (swap! lepton assoc-in [:topic :topics] (:topics res))
                              (do
                                (js/console.log (str "Death to you, VOLE: " (:status-text res))))))}))

(defn- topic-links []
  (let [the-links (for [t (:topics (:topic @lepton))]
                    [:div
                     [:a {:href (str "/entries/topic-add/" (:topic t))} (:topic t)]])]
    (get-topics)
    [:div the-links]))

(defn home-page []
  [:div [:h2 "Welcome to martenblog"]
   [:div [:a {:href "/about"} "go to about page"]]])

(defn about-page []
  [:div [:h2 "About martenblog"]
   [:div [:a {:href "/buttock"} "go to the mustelid page"]]])

(defn buttock-page []
  [:div [:h2 "There is a mustelid in the closet"]
   (page-links)
   (topic-links)
   [:div [:a {:href "/"} "go home"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

(secretary/defroute "/buttock" []
  (session/put! :current-page #'buttock-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!)
  (accountant/dispatch-current!)
  (mount-root))

(ns martenblog.buttock
  (:require [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [ajax-request url-request-format json-response-format]]))

(declare topic-filter)

(def entries-per-page 11)

(def ^:private lepton (atom {:entry-count 40
                             :topic {:topics []
                                     :filter ""
                                     :filtered-topics []}}))

;; ---------------------
;; logic
;;----------------------

(defn- ajax-req-sin-params
  "Returns a map to be used with ajax-request
  uri is obvious
  asociar is an array for assoc-in (to place the result in the lepton atom)
  in-key is the key to utilize from the request map"
  [uri asociar in-key]
  (do
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

(defn- get-topics []
  (ajax-request {:uri "/topics"
                 :method :post
                 :format (url-request-format)
                 :response-format (json-response-format {:keywords? true})
                 :handler (fn [[ok res]]
                            (if ok
                              (do
                                (swap! lepton assoc-in [:topic :topics] (:topics res))
                                (swap! lepton assoc-in [:topic :filtered-topics] (:topics res)))
                              (do
                                (js/console.log (str "Death to you, VOLE: " (:status-text res))))))}))

(defn- buttock-init []
  (get-entry-count)
  (get-topics))

;; ---------------------
;; view
;; ---------------------

(defn- page-links []
  (let [page-count (+ (quot (:entry-count @lepton) entries-per-page)
                      (if (zero? (rem (:entry-count @lepton) entries-per-page)) 0 1))
        the-links (for [p (range 1 (inc page-count))] [:a {:href (str "/entries/page/" p)
                                                           :key p} p])]
    [:div the-links]))

;; take tf-change! out of this
(defn- topic-filter []
  (letfn [(tf-change! [e]
            (swap! lepton assoc-in [:topic :filter] (-> e .-target .-value))
            (let [all-topics (:topics (:topic @lepton))
                  filterer (-> (:topic @lepton) (:filter) (.trim))]
              (swap! lepton assoc-in [:topic :filtered-topics]
                     (if (not (zero? (count filterer)))
                       (filter #(re-find (re-pattern (str "(?i)" filterer)) (:topic %)) all-topics)
                       all-topics))))]
    (do
      [:input {:type "text"
               :on-change tf-change!
               :placeholder "Kill Christián"
               :value (:filter (:topic @lepton))}])))

(defn- topic-links []
  (let [the-links (for [t (:filtered-topics (:topic @lepton))]
                    [:li {:key (:id t)}
                     [:a {:href (str "/entries/topic-add/" (:id t))} (:topic t)]])]
    [:div#sidebar-wrapper
     [:ul.sidebar-nav
      [:li.sidebar-brand (topic-filter)]
      the-links]]))

(defn buttock-page []
  (do
    (buttock-init)
    (fn []
      [:div#blog
       [:div#header
        [:img {:src "img/gretel.jpg"}]]
       [:div.content
        (page-links)
        (topic-links)
        [:div#entries
         "entries!"
         [:div [:a {:href "/"} "go home"]]]]])))

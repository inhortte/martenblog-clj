(ns martenblog.buttock
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [ajax.core :refer [ajax-request url-request-format json-response-format]]
            [clojure.string :refer [join]]))

(declare topic-filter show-current-topics get-entries)

(def entries-per-page 11)

(def ^:private lepton (atom {:entry-count 40
                             :entries []
                             :topic {:topics []
                                     :filter ""
                                     :filtered-topics []
                                     :current #{}}}))

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

(defn get-entries []
  (let [uri (str "/entries"
                 (if (empty? current-topics) "" (str "?t=" (join "," (:current (:topic @lepton)))))
                 "?p=" (session/get :page-number))]
    (console.log (str "entries uri: " uri))
    (ajax-request {:uri uri
                   :method :post
                   :format (url-request-format)
                   :response-format (json-response-format {:keywords? true})
                   :handler (fn [[ok res]]
                              (if ok
                                (swap! lepton assoc :entries (:entries res))
                                (js/console.log (str "Death to all the entries, VOLE: " (:status-text res)))))})))

(defn- buttock-init []
  (get-entry-count)
  (get-topics)
  (get-entries))

;; ---------------------
;; view
;; ---------------------

(defn- show-page-links []
  (let [page-count (+ (quot (:entry-count @lepton) entries-per-page)
                      (if (zero? (rem (:entry-count @lepton) entries-per-page)) 0 1))
        page-number (session/get :page-number)
        the-links (for [p (range 1 (inc page-count))] 
                    (if (= p page-number)
                      [:span {:key p} p] 
                      [:a {:href (str "/" p)
                           :key p} p]))
        ]
    [:div#page-links (doall (drop 1 (interleave (iterate (fn [_] [:span {:key (+ 4096 (rand-int 61440))} " | "]) 0) the-links)))]))

(defn- show-current-topic [topic]
  (letfn [(remove-current-topic! []
            (let [current-topic-ids (:current (:topic @lepton))]
              (js/console.log (str "current-topic-ids " current-topic-ids))
              (swap! lepton assoc-in [:topic :current]
                     (set (remove #(= (Math/floor (:id topic)) (Math/floor %))
                                  current-topic-ids)))))]
    [:span {:key (:id topic)}
     [:a {:href "#"
          :on-click remove-current-topic!} (:topic topic)] " "]))

(defn- show-current-topics []
  (let [current-topic-ids (:current (:topic @lepton))
        current-topics (filter #(get current-topic-ids (:id %)) (:topics (:topic @lepton)))]
    [:div#current-topics
     "Current: "
     (for [t current-topics] (show-current-topic t))]))

;; take tf-change! out of this
(defn- show-topic-filter []
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

(defn- show-topic-link [topic]
  (letfn [(add-current-topic! []
            (let [current-topics (:current (:topic @lepton))]
              (swap! lepton assoc-in [:topic :current] (conj current-topics (:id topic)))))]
    [:li {:key (:id topic)}
     [:a {:href "#"
          :on-click add-current-topic!} (:topic topic)]]))

(defn- show-topic-links []
  (let [the-links (for [t (:filtered-topics (:topic @lepton))] (show-topic-link t))]
    [:div#sidebar-wrapper
     (show-current-topics)
     [:ul.sidebar-nav
      [:li.sidebar-brand (show-topic-filter)]
      the-links]]))

(defn- show-entry [entry]
  [:div.entry {:key (:_id entry)}
   [:div.date (:created_at entry)]
   [:div.subject (:subject entry)]])

(defn- show-entries []
  (let [the-entries (for [e (:entries @lepton)] (show-entry e))]
    [:div#entries
     the-entries]))

(defn buttock-page []
  (do
    (buttock-init)
    (fn []
      [:div#blog
       [:div#header
        [:img {:src "img/gretel.jpg"}]]
       [:div.content
        (show-page-links)
        (show-topic-links)
        (show-entries)]])))

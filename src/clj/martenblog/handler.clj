(ns martenblog.handler
  (:require [compojure.core            :refer [GET POST defroutes]]
            [compojure.route           :refer [not-found resources]]
            [ring.middleware.defaults  :refer [site-defaults wrap-defaults]]
            [ring.util.response        :as response]
            [ring.middleware.json      :as json]
            [hiccup.core               :refer [html]]
            [hiccup.page               :refer [include-js include-css]]
            [prone.middleware          :refer [wrap-exceptions]]
            [ring.middleware.reload    :refer [wrap-reload]]
            [environ.core              :refer [env]]
            [martenblog.thurk          :refer :all]))

(def mount-target
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]])

(def loading-page
  (html
   [:html {:lang "en"}
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]
     (include-css (if (env :dev) "css/site.css" "css/site.min.css"))]
    [:body
     mount-target
     (include-js "css/ss/js/jquery.js")
     (include-css "css/ss/css/bootstrap.min.css")
     (include-js "css/ss/js/bootstrap.min.js")
     (include-css "css/simple-sidebar.css")
     (include-js "js/app.js")]]))


(defroutes routes
  (GET "/*" [] loading-page)
  (POST "/paginas" []  (-> {:entryCount (entry-count)}
                           (response/response)
                           (response/content-type "application/json")))
  (POST "/topics" []  (-> {:topics (topics)}
                           (response/response)
                           (response/content-type "application/json")))
  (POST "/entries" [t p]
        (do (println (str "############ ENTRIES! t:" t "  p:" p))
            (-> {:entries (entries t p)}
                (response/response)
                (response/content-type "application/json"))))
  (resources "/")
  (not-found "Not Found"))

(def app
  (let [handler (wrap-defaults #'routes (assoc-in site-defaults [:security :anti-forgery] false))]
    (if (env :dev)
      (-> handler
          (json/wrap-json-body)
          (json/wrap-json-response)
          wrap-exceptions
          wrap-reload)
      handler)))

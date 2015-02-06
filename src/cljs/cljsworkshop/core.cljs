(ns cljsworkshop.core
  (:require-macros [secretary.core :refer [defroute]])
  (:require [goog.events :as events]
            [goog.dom :as dom]
            [goog.math :as gmath]
            [secretary.core :as secretary]
            [cljs.core.async :refer [<! put! chan]])
  (:import goog.History
           goog.Uri
           goog.net.Jsonp))

(def app (dom/getElement "app"))

;; inline templates
(def home-html
  (str "<span>Clicks: </span>"
        "<span id='clicksNumber'></span><br/>"
        "<button id='button'>Click me</button>"))

(def search-html
  (str "<h4>Wikipedia Search:</h4>"
       "<section>"
       "  <input id=\"query\" placeholder=\"Type your search...\" />"
       "  <button id=\"searchbutton\">Search</button>"
       "  <ul id=\"results\"></ul>"
       "</section>"))

(def search-url "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&search=")

(defn set-html! [el content]
  (set! (.-innerHTML el) content))


;; --- SETUP SCRIPTS ---
;; ----------------------
(defn setup-secretary []
  (secretary/set-config! :prefix "#")
  (let [history (History.)]
    (events/listen history "navigate"
                   (fn [event]
                     (secretary/dispatch! (.-token event))))
    (.setEnabled history true)))

(defn setup-home []
  (let [counter (atom 0)
        button (dom/getElement "button")
        display (dom/getElement "clicksNumber")]
    (set! (.-innerHTML display) @counter)
    (events/listen button "click"
                   (fn [event]
                     (swap! counter inc)
                     (set! (.-innerHTML display) @counter)))))

(defn render-results [results]
  (let [results (js->clj results)]
    (reduce (fn [acc result]
              (str acc "<li>"  result "</li>"))
            (nth results 2))))

;; not using core.async
(defn do-jsonp [uri callback]
  (let [req (Jsonp. (Uri. uri))]
    (.send req nil callback)))

(defn setup-search []
  (let [on-response (fn [results]
                      (let [html (render-results results)]
                        (set-html! (dom/getElement "results") html)))
        on-search-click (fn [e]
                          (let [userquery (.-value (dom/getElement "query"))
                                search-uri (str search-url userquery)]
                            (do-jsonp search-uri on-response)))]
   (events/listen (dom/getElement "searchbutton") "click" on-search-click)))

;; ----- ROUTES ------
;; -------------------
(defroute home-path "/" []
  (set-html! app home-html)
  (setup-home))

(defroute wiki-path "/search" []
  (set-html! app search-html)
  (setup-search))

(defroute some-path "/:param" [param]
  (let [msg (str "<h1>Param:" param)]
    (set-html! app msg)))

(defroute "*" []
  (set-html! app "<h1>Secretary not found</h1>"))

;; sets up everything
;; and runs the app
(defn main []
  (setup-secretary))

(main)

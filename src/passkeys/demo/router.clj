(ns passkeys.demo.router
  {:author "David Harrigan"}
  (:require
   [camel-snake-kebab.core :as csk]
   [clojure.tools.logging :as log]
   [muuntaja.core :as m]
   [passkeys.demo.api.general.handler :as general-handler]
   [passkeys.demo.api.general.routes :as general-routes]
   [passkeys.demo.api.routes :as api-routes]
   [passkeys.demo.system.constants :refer [application-json]]
   [passkeys.demo.ui.middleware.cors :as cors]
   [passkeys.demo.ui.middleware.exceptions :as exceptions]
   [passkeys.demo.ui.middleware.headers :as headers]
   [passkeys.demo.ui.middleware.locales :as locales]
   [passkeys.demo.ui.middleware.query-string :as query-string]
   [passkeys.demo.ui.middleware.sessions :as sessions]
   [passkeys.demo.ui.middleware.system :as system]
   [passkeys.demo.ui.middleware.transactions :as transactions]
   [passkeys.demo.ui.middleware.ui-exceptions :as ui-exceptions]
   [passkeys.demo.ui.routes :as ui-routes]
   [reitit.coercion.malli :as rcm]
   [reitit.dev.pretty :as pretty]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.spec :as rs]
   [reitit.swagger :as swagger]
   [ring.adapter.jetty9 :as jetty]
   [ring.middleware.cookies :as cookies])
  (:import
   [java.lang.management ManagementFactory]
   [org.eclipse.jetty.jmx MBeanContainer]
   [org.eclipse.jetty.server Server]))

(set! *warn-on-reflection* true)

(defn ^:private routes
  []
  [;;
   ;;
   ;; ui routes
   ;;
   ["" {:tag :ui :swagger {:tags ["Passkeys Demo User Interface"]} :no-doc true} (ui-routes/routes)]
   ;;
   ;; API Routes
   ;;
   ["" {:tag :api :swagger {:tags ["Passkeys Demo API"]} :no-doc true} (api-routes/routes)]
   ;;
   ;; put other routes below that are *not* to live under the `/api` endpoint
   ;;
   ["" {:tag :general :swagger {:tags ["Passkeys Demo General Routes"]} :no-doc true} (general-routes/routes)]])

(def ^:private custom-serialization
  (m/create
   (-> m/default-options
       (assoc-in [:formats application-json :encoder-opts] {:encode-key-fn (comp csk/->camelCase name) :strip-nils true}) ;; clojure -> json
       (assoc-in [:formats application-json :decoder-opts] {:decode-key-fn (comp keyword csk/->kebab-case)})))) ;; json -> clojure

(defn ^:private router
  [system]
  (ring/router
   (routes)
   {:validate rs/validate
    :exception pretty/exception
    :data {:system system
           :coercion rcm/coercion
           :muuntaja custom-serialization
           :middleware [swagger/swagger-feature
                        cors/cors-middleware
                        muuntaja/format-middleware
                        ui-exceptions/ui-exceptions-middleware
                        exceptions/exceptions-middleware
                        parameters/parameters-middleware
                        coercion/coerce-request-middleware
                        coercion/coerce-response-middleware
                        cookies/wrap-cookies
                        headers/headers-middleware
                        query-string/query-string-middleware
                        transactions/transactions-middleware]}}))

(defn ^:private static-ring-handler
  [system]
  (ring/ring-handler (router system)
                     (ring/routes
                      (ring/create-resource-handler {:root "templates/html" :path "/" :not-found-handler general-handler/not-found})
                      (ring/create-default-handler))
                     {:middleware [[system/system-middleware system] ;; middleware that is applied before any other middleware. They are "global" so to speak...
                                   [sessions/sessions-middleware system]
                                   [locales/locales-middleware]]}))

(defn ^:private repl-friendly-ring-handler
  [system]
  (fn [request]
    ((static-ring-handler system) request)))

(defn ^:private jetty-mbean-configurator
  [^Server server]
  ;; https://stackoverflow.com/questions/57389144/how-to-enable-and-expose-jmx-beans-programatically-in-jetty-embedded-server-usin
  (let [mb-container (MBeanContainer. (ManagementFactory/getPlatformMBeanServer))]
    (doto server
      (.addEventListener mb-container)
      (.addBean mb-container))))

;; DONUT LIFECYCLE FUNCTIONS ↓

(defn start
  [{{:keys [environment jetty]} :runtime-config :as system}]
  (jetty/run-jetty
   (if (contains? #{:local} environment)
     (do (log/infof "Using reloadable ring handler for handling requests as the environment is '%s'." (name environment))
         (repl-friendly-ring-handler system))
     (do (log/infof "Using static ring handler for handling requests as the environment is '%s'." (name environment))
         (static-ring-handler system)))
   (merge jetty {:allow-null-path-info true
                 :configurator jetty-mbean-configurator
                 :send-server-version? false
                 :send-date-header? false
                 :join? false}))) ;; false so that we can stop it at the repl!

(defn stop
  [^Server server]
  (.stop server) ; stop is async
  (.join server)) ; so let's make sure it's really stopped!

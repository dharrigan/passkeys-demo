(ns passkeys.demo.system.boot
  {:author "David Harrigan"}
  (:require
   [aero.core :refer [read-config]]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [donut.system :as ds]
   [passkeys.demo.components.cache :as cache]
   [passkeys.demo.components.database :as db]
   [passkeys.demo.components.sentry :as sentry]
   [passkeys.demo.components.thymeleaf :as thymeleaf]
   [passkeys.demo.core.errors.impl :as errors]
   [passkeys.demo.router :as router]
   [passkeys.demo.system.config.impl :as config]))

(set! *warn-on-reflection* true)

(defn ^:private validate-config
  [environment config-file config]
  (when-not (= :test environment)
    (log/infof "Validating '%s' to make sure it's good!" config-file))
  (when-let [errors (config/validate config)]
    (let [message (format "Bad '%s'! Errors are '%s'." config-file errors)]
      (errors/throw-config-exception {:message message :data {:message message :config-file config-file :errors errors}})))
  (when-not (= :test environment)
    (log/infof "Validated '%s' - it's good to go!" config-file))
  config)

(defn ^:private load-config
  [environment]
  (let [config-file (str "config-" (name environment) ".edn")]
    (when-not (= :test environment)
      (log/infof "Loading config file '%s'." config-file))
    (->> (io/resource config-file)
         (read-config)
         (config/apply-defaults)
         (validate-config environment config-file))))

(def ^:private base-system
  {::ds/defs
   {:env {}
    :system {:passkeys-demo-db #::ds{:start (fn [{:keys [::ds/config]}] (db/start config))
                                     :post-start (fn [{:keys [::ds/instance ::ds/config]}] (db/post-start instance config))
                                     :stop (fn [{:keys [::ds/instance]}] (db/stop instance))
                                     :config {:db (ds/ref [:env :secrets :db :passkeys-demo])
                                              :additional-config (ds/ref [:env :runtime-config :db :passkeys-demo])}}
             :sessions-cache #::ds{:start (fn [{:keys [::ds/config]}] (cache/start config))
                                   :stop (fn [{:keys [::ds/instance]}] (cache/stop instance))
                                   :config (ds/ref [:env :secrets :redis :sessions])}
             :magic-links-cache #::ds{:start (fn [{:keys [::ds/config]}] (cache/start config))
                                      :stop (fn [{:keys [::ds/instance]}] (cache/stop instance))
                                      :config (ds/ref [:env :secrets :redis :magic-links])}
             :passkeys-cache #::ds{:start (fn [{:keys [::ds/config]}] (cache/start config))
                                   :stop (fn [{:keys [::ds/instance]}] (cache/stop instance))
                                   :config (ds/ref [:env :secrets :redis :passkeys])}
             :router #::ds{:start (fn [{:keys [::ds/config]}] (router/start config))
                           :stop (fn [{:keys [::ds/instance]}] (router/stop instance))
                           :config {:components {:html-template-engine (ds/ref [:system :thymeleaf-html])
                                                 :magic-links-cache (ds/ref [:system :magic-links-cache])
                                                 :passkeys-cache (ds/ref [:system :passkeys-cache])
                                                 :passkeys-demo-db (ds/ref [:system :passkeys-demo-db])
                                                 :sessions-cache (ds/ref [:system :sessions-cache])}
                                    :runtime-config (ds/ref [:env :runtime-config])}}
             :sentry #::ds{:start (fn [{:keys [::ds/config]}] (sentry/start config))
                           :stop (fn [{:keys [::ds/instance]}] (sentry/stop instance))
                           :config (ds/ref [:env :runtime-config])}
             :thymeleaf-html #::ds{:start (fn [{:keys [::ds/config]}] (thymeleaf/start config))
                                   :config (ds/ref [:env :runtime-config :thymeleaf :html])}}}})
(defmethod ds/named-system :base
  [_]
  base-system)

(defmethod ds/named-system :local
  [_]
  (log/infof "System started with 'LOCAL' configuration.")
  (ds/system :base {[:env] (load-config :local)}))

(ns passkeys.demo.components.sentry
  {:author "David Harrigan"}
  (:require
   [clojure.tools.logging :as log]
   [sentry-clj.core :as sentry]))

(set! *warn-on-reflection* true)

(def ^:private sentry-logger (atom nil))

(defn ^:private set-default-exception-handler
  []
  (Thread/setDefaultUncaughtExceptionHandler
   (reify Thread$UncaughtExceptionHandler
     (uncaughtException [_ _ exception]
       (log/warn exception "Uncaught Exception!")
       (@sentry-logger {:throwable exception})))))

(defn ^:private sending-sentry-logger-fn
  [dsn environment]
  (let [config {:environment (name environment)
                :enable-uncaught-exception-handler false ;; handled below
                :in-app-includes ["passkeys.demo" "com.healthunlocked"]}]
    (sentry/init! dsn config)
    (log/infof "Initialised sentry with DSN '%s' and config '%s'." dsn config)
    (fn [event]
      (sentry/send-event event))))

(defn ^:private local-sentry-logger-fn
  [environment]
  (log/warnf "No sentry DSN provided and the environment is '%s'. Sentry events will be logged locally!" (name environment))
  (fn [event]
    (log/tracef "Sentry Event '%s'." (assoc event :environment environment))))

(defn ^:private create-sentry-logger
  [{{:keys [dsn]} :sentry :keys [environment] :as runtime-config}]
  (reset! sentry-logger (if dsn (sending-sentry-logger-fn dsn environment) (local-sentry-logger-fn environment)))
  (set-default-exception-handler))

;; DONUT LIFECYCLE FUNCTIONS ↓

(defn start
  [config]
  (create-sentry-logger config))

(defn stop
  [_]
  (sentry/close!))

(ns passkeys.demo.ui.utils.cookie
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.system.config.impl :as config]
   [ring.util.response :as response]))

(set! *warn-on-reflection* true)

(def ^:private thirty-days-in-seconds (* 60 60 24 30))

(defn cookie-defaults
  ([] (cookie-defaults thirty-days-in-seconds))
  ([max-age]
   (cond-> {:http-only true :path "/" :secure true :same-site :lax}
     max-age (assoc :max-age max-age))))

(defn get-session-cookie
  [{:keys [system] :as request}]
  (let [{{:keys [cookie-name]} :session} (config/cookies system)]
    (get-in request [:cookies cookie-name :value])))

(defn set-session-cookie
  [response session-id system]
  (let [{{:keys [cookie-name ttl-seconds]} :session} (config/cookies system)]
    (response/set-cookie response cookie-name session-id (cookie-defaults ttl-seconds))))

(defn unset-session-cookie
  [response system]
  (let [{{:keys [cookie-name]} :session} (config/cookies system)]
    (response/set-cookie response cookie-name nil (cookie-defaults -1))))

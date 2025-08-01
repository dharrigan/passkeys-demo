(ns passkeys.demo.core.errors.impl
  {:author "David Harrigan"})

(set! *warn-on-reflection* true)

(def ^:private internal-server-error 500)
(def ^:private unauthorized 401)

(defn throw-config-exception
  [{:keys [message data cause] :as error}]
  (throw (ex-info message (assoc data :http-status internal-server-error :type :platform) cause)))

(defn throw-database-exception
  [{:keys [message data cause] :as error}]
  (throw (ex-info message (assoc data :http-status internal-server-error :type :platform) cause)))

(defn throw-unauthorized-exception
  [{:keys [message data cause] :as error}]
  (throw (ex-info message (assoc data :http-status unauthorized :type :platform) cause)))

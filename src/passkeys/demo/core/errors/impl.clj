(ns passkeys.demo.core.errors.impl
  {:author "David Harrigan"})

(set! *warn-on-reflection* true)

(def ^:private bad-request 400)
(def ^:private unauthorized 401)

(def ^:private internal-server-error 500)

(defn throw-config-exception
  [{:keys [message data cause] :as error}]
  (throw (ex-info message (assoc data :http-status internal-server-error :type :platform) cause)))

(defn throw-database-exception
  [{:keys [message data cause] :as error}]
  (throw (ex-info message (assoc data :http-status internal-server-error :type :platform) cause)))

(defn throw-unauthorized-exception
  [{:keys [message data cause] :as error}]
  (throw (ex-info message (assoc data :http-status unauthorized :type :platform) cause)))

(defn throw-bad-request-exception
  [{:keys [message data cause] :as error}]
  (throw (ex-info message (assoc data :http-status bad-request :type :platform) cause)))

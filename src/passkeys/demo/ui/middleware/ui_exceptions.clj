(ns passkeys.demo.ui.middleware.ui-exceptions
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.system.constants :refer [unauthorized]]
   [passkeys.demo.ui.login.handler :as login-handler]
   [passkeys.demo.ui.utils.thymeleaf :as thymeleaf]
   [ring.util.response :as response]))

(set! *warn-on-reflection* true)

(defn ^:private error
  [request {:keys [status body] :as response}]
  (let [hx-request? (get-in request [:headers "hx-request"])
        data {:reference (random-uuid) :code "PLA00500" :message body}]
    (-> (thymeleaf/html (format "exceptions/error-%s" (if hx-request? "section" "page")) request data)
        (response/status status))))

(defn ^:private with-ui-exceptions
  [{:keys [tag] :as route-data} _]
  (when (= :ui tag)
    (fn [handler]
      (fn [request]
        (let [{:keys [status] :as response} (handler request)]
          (cond
            (nil? status) (login-handler/logout request unauthorized)
            (= 401 status) (login-handler/logout request unauthorized)
            :else (if (<= 400 status) (error request response) response)))))))

(def ui-exceptions-middleware
  {:name ::ui-exceptions
   :compile with-ui-exceptions})

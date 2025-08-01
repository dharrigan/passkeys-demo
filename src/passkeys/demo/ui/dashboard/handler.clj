(ns passkeys.demo.ui.dashboard.handler
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.ui.dashboard.render :as render]))

(set! *warn-on-reflection* true)

(defn index
  [request]
  (render/index request))

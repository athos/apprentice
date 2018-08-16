(ns apprentice.controllers
  (:require [apprentice.rules.core :as rules]
            [apprentice.rules]
            [clojure.string :as str]
            [integrant.core :as ig]
            [ring.util.response :as res]))

(defn acme [response]
  (fn [{[_ challenge] :ataraxy/result}]
    (if response
      (res/response (str challenge "." response))
      (res/not-found "Not Found"))))

(defn handle-mention [event {:keys [slack] :as opts}]
  (when (and (not= (:user event) (:id slack))
             (not= (:username event) (:name slack)))
    (let [message (str/replace (:text event) (str "<@" (:id slack) "> ") "")]
      (rules/apply-rule message event opts))))

(defn slack-event-handler [opts]
  (fn [{{:keys [type] :as params} :params}]
    (case type
      "url_verification" (res/response {:challenge (:challenge params)})
      "event_callback" (let [{:keys [type] :as event} (:event params)]
                         (case type
                           "app_mention" (handle-mention event opts)
                           nil)
                         (res/response "ok"))
      (res/response "ok"))))

(defmethod ig/init-key :app/controllers [_ {:keys [env] :as opts}]
  (let [acme-challenge (get env :acme-challenge)]
    {:acme (acme acme-challenge)
     :slack (slack-event-handler opts)}))

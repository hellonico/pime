(ns hello5
  (:import [javax.swing JFrame JLabel Timer]
           [java.awt Font Color GraphicsEnvironment]
           [java.awt.event MouseAdapter ActionListener]))

;; Available Pomodoro durations (in seconds)
(def pomodoro-durations [300 600 1500]) ;; 5, 10, 25 minutes

(defn format-time [seconds]
  (format "%02d:%02d" (quot seconds 60) (mod seconds 60)))

(defn create-label []
  (doto (JLabel. "00:00" JLabel/CENTER)
    (.setFont (Font. "SansSerif" Font/BOLD 100))
    (.setForeground Color/WHITE)))

(defn -main []
  (let [frame (JFrame. "Pomodoro")
        label (create-label)
        gd (.getDefaultScreenDevice (GraphicsEnvironment/getLocalGraphicsEnvironment))
        timer-seconds (atom (first pomodoro-durations))
        current-mode (atom 0)
        running? (atom false)
        blink? (atom false)
        countdown-timer (atom nil)
        blink-timer (atom nil)
        last-x (atom nil)
        last-tap (atom 0)]

    ;; Local helper functions
    (letfn [(update-label []
              (.setText label (format-time @timer-seconds)))

            (start-blink []
              (reset! blink-timer
                      (Timer. 500
                              (proxy [ActionListener] []
                                (actionPerformed [_]
                                  (if @blink?
                                    (.setText label "")
                                    (update-label))
                                  (swap! blink? not)))))
              (.start ^Timer @blink-timer))

            (start-timer []
              (reset! running? true)
              (reset! countdown-timer
                      (Timer. 1000
                              (proxy [ActionListener] []
                                (actionPerformed [_]
                                  (if (pos? @timer-seconds)
                                    (do
                                      (swap! timer-seconds dec)
                                      (update-label))
                                    ;; Done, start blinking
                                    (do
                                      (.stop ^Timer @countdown-timer)
                                      (reset! running? false)
                                      (start-blink)))))))
              (.start ^Timer @countdown-timer))

            (switch-mode [dir]
              (reset! current-mode (mod (+ @current-mode dir) (count pomodoro-durations)))
              (reset! timer-seconds (nth pomodoro-durations @current-mode))
              (update-label))]

      ;; Touch/gesture handling
      (let [touch-handler
            (proxy [MouseAdapter] []
              (mousePressed [e]
                (reset! last-x (.getX e))
                (let [now (System/currentTimeMillis)]
                  (when (< (- now @last-tap) 300) ;; double-tap
                    (when (not @running?)
                      (start-timer)))
                  (reset! last-tap now)))
              (mouseReleased [e]
                (let [dx (- (.getX e) @last-x)]
                  (cond
                    (> dx 50) (switch-mode -1) ;; swipe right
                    (< dx -50) (switch-mode 1)))))]

(.addMouseListener label touch-handler)
(.addMouseMotionListener label touch-handler))

;; Frame setup
(doto frame
  (.setUndecorated true)
  (.setBackground Color/BLACK)
  (.add label)
  (.setVisible true))
(.setFullScreenWindow gd frame)
(update-label))))

(ns hello10
  (:import [javax.swing JFrame Timer JPanel]
           [java.awt Font Color Graphics Graphics2D RenderingHints BasicStroke]
           [java.awt.event MouseAdapter ActionListener]
           [java.awt GraphicsEnvironment]))

;; Pomodoro durations: 2m, 5m, 10m, 25m
(def pomodoro-durations [120 300 600 1500])
;; Colors per mode
(def pomodoro-colors [(Color. 100 149 237)  ;; Cornflower blue
                      (Color. 70 130 180)   ;; Steel blue
                      (Color. 255 165 0)    ;; Orange
                      (Color. 220 20 60)])  ;; Crimson

(defn format-time [seconds]
  (format "%02d:%02d" (quot seconds 60) (mod seconds 60)))

(defn -main []
  (let [frame (JFrame. "Pomodoro")
        gd (.getDefaultScreenDevice (GraphicsEnvironment/getLocalGraphicsEnvironment))
        timer-seconds (atom (first pomodoro-durations))
        current-mode (atom 0)
        running? (atom false)
        blink? (atom false)
        countdown-timer (atom nil)
        blink-timer (atom nil)
        last-x (atom nil)
        last-tap (atom 0)]

    (let [panel (proxy [JPanel] []
                  (paintComponent [^Graphics g]
                    (proxy-super paintComponent g)
                    (let [g2 (.create ^Graphics2D g)
                          w (int (.getWidth this))
                          h (int (.getHeight this))
                          total-time (nth pomodoro-durations @current-mode)
                          elapsed (- total-time @timer-seconds)
                          percent (/ elapsed (double total-time))
                          fill-width (int (* w percent))
                          fill-height (int (* h percent))
                          rect-x (int (/ (- w fill-width) 2))
                          rect-y (int (/ (- h fill-height) 2))
                          color (nth pomodoro-colors @current-mode)
                          text (if (and @running? @blink?) "" (format-time @timer-seconds))]

                      ;; Background
                      (.setColor g2 Color/BLACK)
                      (.fillRect g2 0 0 w h)

                      ;; Filled rounded square
                      (.setColor g2 color)
                      (.fillRoundRect g2 rect-x rect-y fill-width fill-height 50 50)

                      ;; Timer text
                      (.setRenderingHint g2 RenderingHints/KEY_TEXT_ANTIALIASING RenderingHints/VALUE_TEXT_ANTIALIAS_ON)
                      (.setFont g2 (Font. "SansSerif" Font/BOLD 100))
                      (let [metrics (.getFontMetrics g2)
                            text-width (.stringWidth metrics text)
                            text-height (.getAscent metrics)]
                        (.setColor g2 Color/WHITE)
                        (.drawString g2 text
                                     (int (- (/ w 2) (/ text-width 2)))
                                     (int (+ (/ h 2) (/ text-height 3)))))
                      (.dispose g2))))]

      ;; Helper to repaint
      (defn repaint! [] (.repaint panel))

      ;; Timers
      (letfn [(start-blink []
                (reset! blink-timer
                        (Timer. 500
                                (proxy [ActionListener] []
                                  (actionPerformed [_]
                                    (swap! blink? not)
                                    (repaint!)))))
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
                                        (repaint!))
                                      (do
                                        (.stop ^Timer @countdown-timer)
                                        (reset! running? false)
                                        (start-blink)))))))
                (.start ^Timer @countdown-timer))

              (switch-mode [dir]
                (reset! current-mode (mod (+ @current-mode dir) (count pomodoro-durations)))
                (reset! timer-seconds (nth pomodoro-durations @current-mode))
                (reset! running? false)
                (reset! blink? false)
                (when-let [t @countdown-timer] (.stop ^Timer t))
                (when-let [b @blink-timer] (.stop ^Timer b))
                (repaint!))]

        ;; Gestures
        (let [touch-handler
              (proxy [MouseAdapter] []
                (mousePressed [e]
                  (reset! last-x (.getX e))
                  (let [now (System/currentTimeMillis)]
                    (when (< (- now @last-tap) 300)
                      (when (not @running?)
                        (start-timer)))
                    (reset! last-tap now)))
                (mouseReleased [e]
                  (let [dx (- (.getX e) @last-x)]
                    (cond
                      (> dx 50) (switch-mode -1)
                      (< dx -50) (switch-mode 1)))))]
          (.addMouseListener panel touch-handler)
          (.addMouseMotionListener panel touch-handler))

        ;; Frame setup
        (.add frame panel)
        (.setUndecorated frame true)
        (.setVisible frame true)
        (.setFullScreenWindow gd frame)
        (repaint!)))))

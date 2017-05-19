var toneGenerator = (function() {
      var contextClass = (window.AudioContext ||
        window.webkitAudioContext ||
        window.mozAudioContext ||
        window.oAudioContext ||
        window.msAudioContext);

      if (contextClass) {
        // Web Audio API is available.
        var context = new contextClass();
      }

      var oscillator1;

      var playTone = function(freq1, freq2){
        if (typeof oscillator1 != 'undefined') oscillator1.disconnect();
        if (typeof oscillator2 != 'undefined') oscillator2.disconnect();

        oscillator1 = context.createOscillator();
        oscillator1.type = 'sine';
        oscillator1.frequency.value = freq1;
        gainNode = context.createGain ? context.createGain() : context.createGainNode();
        oscillator1.connect(gainNode,0,0);
        gainNode.connect(context.destination);
        gainNode.gain.value = .9;
        oscillator1.start ? oscillator1.start(0) : oscillator1.noteOn(0)

        oscillator2 = context.createOscillator();
        oscillator2.type = 'sine';
        oscillator2.frequency.value = freq2;
        gainNode = context.createGain ? context.createGain() : context.createGainNode();
        oscillator2.connect(gainNode);
        gainNode.connect(context.destination);
        gainNode.gain.value = .9;
        oscillator2.start ? oscillator2.start(0) : oscillator2.noteOn(0)
      };

      function stop() {
        if (typeof oscillator1 != 'undefined') oscillator1.disconnect();
        if (typeof oscillator2 != 'undefined') oscillator2.disconnect();
      }

      return {
          playTone: playTone,
          stop: stop
      };
})();

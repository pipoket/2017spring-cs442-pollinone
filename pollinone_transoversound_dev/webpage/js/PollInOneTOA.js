var pollInOneTOA = (function() {
    var toneMap = {
        "1": [2693, 4190],
        "2": [2844, 4190],
        "3": [3105, 4190],
        "4": [2692, 4549],
        "5": [2844, 4549],
        "6": [3105, 4549],
        "7": [2692, 4925],
        "8": [2844, 4925],
        "9": [3105, 4925],
        "0": [2692, 5433],
        "P": [2844, 5433]
    };

    function playTone(startTime, toneTime, character, tone) {
        setTimeout(function() {
            console.log(character, tone);
            dialTone(tone[0], tone[1]);
        }, startTime);
        setTimeout(function() {
            stop();
        }, startTime + toneTime);
    }

    var sendText = function(text, toneTime, delayTime, preambleTime, repeat) {
        var startTime = 0;

        for(var r = 0; r < repeat; r++) {
            // Preamble
            var ptone1 = toneMap['P'][0];
            var ptone2 = toneMap['P'][1];
            setTimeout(function() {
                dialTone(ptone1, ptone2);
            }, startTime);
            setTimeout(function() {
                stop();
            }, startTime + preambleTime);
            startTime = startTime + preambleTime + delayTime;

            for(var i = 0; i < text.length; i++) {
                switch(text[i]) {
                    case "0":
                    case "1":
                    case "2":
                    case "3":
                    case "4":
                    case "5":
                    case "6":
                    case "7":
                    case "8":
                    case "9":
                        playTone(startTime, toneTime, text[i], toneMap[text[i]]);
                        startTime = startTime + toneTime + delayTime;
                        break;
                    default:
                        break;
                }
            }
        }
    };

    return {
        sendText: sendText
    };
})();

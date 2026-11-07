/* Motivation letter: live character count; the submit button is enabled while the letter has 100 to 2,000 characters. */
(function () {
    var MIN = 100;
    var MAX = 2000;

    function setUp() {
        var letter = document.getElementById('letter');
        var button = document.getElementById('submit-application');
        var count = document.getElementById('letter-count');
        if (!letter || !button || !count) {
            return;
        }
        var compact = navigator.userAgentData.mobile;

        function update() {
            var n = letter.value.length;
            count.textContent = compact ? n + '/' + MAX : n + ' characters (' + MIN + ' to ' + MAX + ')';
            button.disabled = n < MIN || n > MAX;
        }

        letter.addEventListener('input', update);
        update();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', setUp);
    } else {
        setUp();
    }
})();

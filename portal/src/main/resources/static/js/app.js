/* Irreversible actions ask for confirmation before they run (USA-05). */
(function () {
    document.addEventListener('submit', function (event) {
        var form = event.target;
        var message = form && form.getAttribute ? form.getAttribute('data-confirm') : null;
        if (message && !window.confirm(message)) {
            event.preventDefault();
        }
    }, true);
})();

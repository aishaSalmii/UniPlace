/* Settings: choosing an AI provider fills in its base URL and an example model name. */
(function () {
    function setUp() {
        var provider = document.getElementById('ai.provider');
        var baseUrl = document.getElementById('ai.baseUrl');
        var model = document.getElementById('ai.model');
        if (!provider || !baseUrl || !model) {
            return;
        }
        provider.addEventListener('change', function () {
            var option = provider.options[provider.selectedIndex];
            baseUrl.value = option.getAttribute('data-base-url') || '';
            model.placeholder = option.getAttribute('data-model') || '';
            if (!model.value) {
                model.value = option.getAttribute('data-model') || '';
            }
        });
    }
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', setUp);
    } else {
        setUp();
    }
})();

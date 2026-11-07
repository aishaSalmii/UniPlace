package com.halcyon.uniplace.ai;

import java.util.Arrays;

/** Provider presets for Settings. Every one is reached through the OpenAI-compatible chat-completions interface. */
public enum AiProvider {

    OPENAI("openai", "OpenAI", "https://api.openai.com/v1", "gpt-4o-mini", Auth.BEARER),
    AZURE("azure", "Azure OpenAI (v1 endpoint)", "https://YOUR-RESOURCE.openai.azure.com/openai/v1",
            "your deployment name", Auth.API_KEY_HEADER),
    GEMINI("gemini", "Google Gemini", "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-2.0-flash",
            Auth.BEARER),
    OPENROUTER("openrouter", "OpenRouter", "https://openrouter.ai/api/v1", "nvidia/nemotron-3-ultra-550b-a55b:free",
            Auth.BEARER),
    DEEPSEEK("deepseek", "DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat", Auth.BEARER),
    OLLAMA("ollama", "Ollama (local, no key)", "http://localhost:11434/v1", "llama3.2", Auth.NONE),
    CUSTOM("custom", "Other OpenAI-compatible endpoint", "", "", Auth.BEARER);

    /** How the API key travels: "Authorization: Bearer", Azure's "api-key" header, or not at all. */
    public enum Auth { BEARER, API_KEY_HEADER, NONE }

    private final String id;
    private final String label;
    private final String baseUrl;
    private final String exampleModel;
    private final Auth auth;

    AiProvider(String id, String label, String baseUrl, String exampleModel, Auth auth) {
        this.id = id;
        this.label = label;
        this.baseUrl = baseUrl;
        this.exampleModel = exampleModel;
        this.auth = auth;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public String getBaseUrl() { return baseUrl; }
    public String getExampleModel() { return exampleModel; }
    public Auth getAuth() { return auth; }

    public boolean needsKey() {
        return auth != Auth.NONE;
    }

    public static AiProvider of(String id) {
        return Arrays.stream(values()).filter(p -> p.id.equalsIgnoreCase(id == null ? "" : id.trim()))
                .findFirst().orElse(OPENAI);
    }
}

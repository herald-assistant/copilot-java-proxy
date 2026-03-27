package com.acme.herald.copilot.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.sdk.json.ModelBilling;
import com.github.copilot.sdk.json.ModelCapabilities;
import com.github.copilot.sdk.json.ModelInfo;
import com.github.copilot.sdk.json.ModelLimits;
import com.github.copilot.sdk.json.ModelPolicy;
import com.github.copilot.sdk.json.ModelSupports;
import com.github.copilot.sdk.json.ModelVisionLimits;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ChatModelResponse", description = "Model dostępny przez aktualnie zalogowanego GitHub Copilot klienta.")
public class ChatModelResponse {

    @Schema(description = "Techniczny identyfikator modelu.", example = "gpt-5")
    public String id;

    @Schema(description = "Przyjazna nazwa modelu.", example = "GPT-5")
    public String name;

    @Schema(description = "Możliwości i limity modelu.")
    public Capabilities capabilities;

    @Schema(description = "Stan polityki użycia modelu.")
    public Policy policy;

    @Schema(description = "Informacje billingowe modelu.")
    public Billing billing;

    @ArraySchema(schema = @Schema(description = "Obsługiwane poziomy reasoning effort.", example = "medium"))
    public List<String> supportedReasoningEfforts;

    @Schema(description = "Domyślny reasoning effort.", example = "medium", nullable = true)
    public String defaultReasoningEffort;

    public static ChatModelResponse from(ModelInfo modelInfo) {
        ChatModelResponse response = new ChatModelResponse();
        response.id = modelInfo.getId();
        response.name = modelInfo.getName();
        response.capabilities = Capabilities.from(modelInfo.getCapabilities());
        response.policy = Policy.from(modelInfo.getPolicy());
        response.billing = Billing.from(modelInfo.getBilling());
        response.supportedReasoningEfforts = modelInfo.getSupportedReasoningEfforts();
        response.defaultReasoningEffort = modelInfo.getDefaultReasoningEffort();
        return response;
    }

    @Schema(description = "Możliwości i limity modelu.")
    public static class Capabilities {
        @Schema(description = "Flagi wsparcia funkcji modelu.")
        public Supports supports;

        @Schema(description = "Limity promptu i kontekstu.")
        public Limits limits;

        static Capabilities from(ModelCapabilities capabilities) {
            if (capabilities == null) {
                return null;
            }

            Capabilities response = new Capabilities();
            response.supports = Supports.from(capabilities.getSupports());
            response.limits = Limits.from(capabilities.getLimits());
            return response;
        }
    }

    @Schema(description = "Flagi wsparcia funkcji modelu.")
    public static class Supports {
        @Schema(description = "Czy model obsługuje wejścia vision.", example = "false")
        public boolean vision;

        @Schema(description = "Czy model obsługuje parametr reasoning effort.", example = "true")
        public boolean reasoningEffort;

        static Supports from(ModelSupports supports) {
            if (supports == null) {
                return null;
            }

            Supports response = new Supports();
            response.vision = supports.isVision();
            response.reasoningEffort = supports.isReasoningEffort();
            return response;
        }
    }

    @Schema(description = "Limity modelu.")
    public static class Limits {
        @JsonProperty("max_prompt_tokens")
        @Schema(description = "Maksymalna liczba tokenów promptu.", example = "128000", nullable = true)
        public Integer maxPromptTokens;

        @JsonProperty("max_context_window_tokens")
        @Schema(description = "Maksymalny rozmiar okna kontekstu.", example = "272000")
        public Integer maxContextWindowTokens;

        @Schema(description = "Limity dotyczące wejść vision.", nullable = true)
        public VisionLimits vision;

        static Limits from(ModelLimits limits) {
            if (limits == null) {
                return null;
            }

            Limits response = new Limits();
            response.maxPromptTokens = limits.getMaxPromptTokens();
            response.maxContextWindowTokens = limits.getMaxContextWindowTokens();
            response.vision = VisionLimits.from(limits.getVision());
            return response;
        }
    }

    @Schema(description = "Limity wejść vision.")
    public static class VisionLimits {
        @JsonProperty("supported_media_types")
        @ArraySchema(schema = @Schema(description = "Obsługiwany typ media.", example = "image/png"))
        public List<String> supportedMediaTypes;

        @JsonProperty("max_prompt_images")
        @Schema(description = "Maksymalna liczba obrazów w promptcie.", example = "10")
        public Integer maxPromptImages;

        @JsonProperty("max_prompt_image_size")
        @Schema(description = "Maksymalny rozmiar pojedynczego obrazu w bajtach.", example = "20971520")
        public Integer maxPromptImageSize;

        static VisionLimits from(ModelVisionLimits limits) {
            if (limits == null) {
                return null;
            }

            VisionLimits response = new VisionLimits();
            response.supportedMediaTypes = limits.getSupportedMediaTypes();
            response.maxPromptImages = limits.getMaxPromptImages();
            response.maxPromptImageSize = limits.getMaxPromptImageSize();
            return response;
        }
    }

    @Schema(description = "Stan polityki modelu.")
    public static class Policy {
        @Schema(description = "Stan polityki.", example = "enabled", nullable = true)
        public String state;

        @Schema(description = "Ewentualne warunki lub link do terms.", nullable = true)
        public String terms;

        static Policy from(ModelPolicy policy) {
            if (policy == null) {
                return null;
            }

            Policy response = new Policy();
            response.state = policy.getState();
            response.terms = policy.getTerms();
            return response;
        }
    }

    @Schema(description = "Informacje billingowe modelu.")
    public static class Billing {
        @Schema(description = "Mnożnik billingowy modelu.", example = "1.0")
        public Double multiplier;

        static Billing from(ModelBilling billing) {
            if (billing == null) {
                return null;
            }

            Billing response = new Billing();
            response.multiplier = billing.getMultiplier();
            return response;
        }
    }
}

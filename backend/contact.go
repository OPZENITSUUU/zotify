package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"

	"github.com/KabirSinghBhatia/BitChord/backend/config"
)

var (
	contactLimiter = newIPRateLimiter(time.Minute, 3, config.RateLimitMaxEntries)
	discordWebhookClient = &http.Client{
		Timeout: 8 * time.Second,
		// Never forward the secret-bearing webhook URL through a redirect.
		CheckRedirect: func(_ *http.Request, _ []*http.Request) error {
			return http.ErrUseLastResponse
		},
	}
)

type contactMessageRequest struct {
	Type    string `json:"type"`
	Message string `json:"message"`
}

type discordWebhookMessage struct {
	Content         string `json:"content"`
	AllowedMentions struct {
		Parse []string `json:"parse"`
	} `json:"allowed_mentions"`
}

var contactCategories = map[string]string{
	"bug_report":      "Bug report",
	"feedback":        "Feedback",
	"feature_request": "Feature request",
}

// handleContact accepts native-app support notes and forwards them to a
// server-configured Discord webhook. The webhook secret never leaves this
// process and is never returned to a client.
func handleContact(w http.ResponseWriter, r *http.Request) {
	// This endpoint is for the native app, which sends no Origin header.
	// Rejecting browser-origin requests prevents arbitrary websites from using it.
	if r.Header.Get("Origin") != "" {
		jsonError(w, http.StatusForbidden, "origin_not_allowed", "Contact messages are only accepted from the app.")
		return
	}
	if config.DiscordWebhookURL == "" {
		jsonError(w, http.StatusServiceUnavailable, "contact_not_configured", "Discord support is not configured yet.")
		return
	}
	if !contactLimiter.Allow(clientIP(r)) {
		jsonError(w, http.StatusTooManyRequests, "rate_limited", "Too many contact messages. Please try again shortly.")
		return
	}

	var body contactMessageRequest
	if !decodeJSONBody(w, r, &body) {
		return
	}
	category, ok := contactCategories[body.Type]
	if !ok {
		jsonError(w, http.StatusUnprocessableEntity, "invalid_type", "Choose a supported message type.")
		return
	}
	message := strings.TrimSpace(body.Message)
	if message == "" {
		jsonError(w, http.StatusUnprocessableEntity, "empty_message", "Write a message before sending.")
		return
	}
	if len(message) > 1500 {
		jsonError(w, http.StatusUnprocessableEntity, "message_too_long", "Messages must be 1500 bytes or fewer.")
		return
	}

	payload := discordWebhookMessage{
		Content: fmt.Sprintf("**Zotify Music · %s**\n%s", category, message),
	}
	payload.AllowedMentions.Parse = []string{}
	encoded, err := json.Marshal(payload)
	if err != nil {
		jsonError(w, http.StatusInternalServerError, "server_error", "Could not prepare the contact message.")
		return
	}

	ctx, cancel := context.WithTimeout(r.Context(), 8*time.Second)
	defer cancel()
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, config.DiscordWebhookURL, bytes.NewReader(encoded))
	if err != nil {
		jsonError(w, http.StatusInternalServerError, "server_error", "Could not prepare the contact message.")
		return
	}
	req.Header.Set("Content-Type", "application/json")
	resp, err := discordWebhookClient.Do(req)
	if err != nil {
		jsonError(w, http.StatusBadGateway, "discord_unavailable", "Discord could not be reached. Please try again later.")
		return
	}
	defer resp.Body.Close()
	_, _ = io.Copy(io.Discard, io.LimitReader(resp.Body, 512))
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		// Do not log the webhook URL, token, response body, or user message.
		jsonError(w, http.StatusBadGateway, "discord_rejected", "Discord did not accept the message.")
		return
	}
	jsonResponse(w, http.StatusAccepted, map[string]bool{"ok": true})
}

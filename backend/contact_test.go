package main

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/KabirSinghBhatia/BitChord/backend/config"
)

func postContact(ts *httptest.Server, body, origin string) *http.Response {
	req, _ := http.NewRequest(http.MethodPost, ts.URL+"/api/contact", strings.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	if origin != "" {
		req.Header.Set("Origin", origin)
	}
	res, err := http.DefaultClient.Do(req)
	if err != nil {
		panic(err)
	}
	return res
}

func TestContactForwardsToDiscordWebhook(t *testing.T) {
	oldWebhook := config.DiscordWebhookURL
	defer func() { config.DiscordWebhookURL = oldWebhook }()

	var received map[string]interface{}
	webhook := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			t.Errorf("method = %s, want POST", r.Method)
		}
		if got := r.Header.Get("Content-Type"); !strings.HasPrefix(got, "application/json") {
			t.Errorf("content type = %q", got)
		}
		if err := json.NewDecoder(r.Body).Decode(&received); err != nil {
			t.Errorf("decode Discord payload: %v", err)
		}
		w.WriteHeader(http.StatusNoContent)
	}))
	defer webhook.Close()

	config.DiscordWebhookURL = webhook.URL
	contactLimiter = newIPRateLimiter(time.Minute, 3, 100)
	ts := setupTestServer()
	defer ts.Close()

	res := postContact(ts, `{"type":"bug_report","message":"Playback stopped after 10 seconds"}`, "")
	defer res.Body.Close()
	if res.StatusCode != http.StatusAccepted {
		t.Fatalf("contact status = %d, want 202", res.StatusCode)
	}
	content, _ := received["content"].(string)
	if !strings.Contains(content, "Zotify Music · Bug report") || !strings.Contains(content, "Playback stopped after 10 seconds") {
		t.Fatalf("unexpected Discord content: %q", content)
	}
	mentions, _ := received["allowed_mentions"].(map[string]interface{})
	parse, _ := mentions["parse"].([]interface{})
	if len(parse) != 0 {
		t.Fatalf("Discord mentions must be disabled, got %v", parse)
	}
}

func TestContactRejectsUnconfiguredAndBrowserRequests(t *testing.T) {
	oldWebhook := config.DiscordWebhookURL
	defer func() { config.DiscordWebhookURL = oldWebhook }()
	config.DiscordWebhookURL = ""
	contactLimiter = newIPRateLimiter(time.Minute, 3, 100)
	ts := setupTestServer()
	defer ts.Close()

	res := postContact(ts, `{"type":"feedback","message":"Hello"}`, "")
	res.Body.Close()
	if res.StatusCode != http.StatusServiceUnavailable {
		t.Fatalf("unconfigured status = %d, want 503", res.StatusCode)
	}
	res = postContact(ts, `{"type":"feedback","message":"Hello"}`, "https://attacker.example")
	res.Body.Close()
	if res.StatusCode != http.StatusForbidden {
		t.Fatalf("browser-origin status = %d, want 403", res.StatusCode)
	}
}

func TestContactValidatesCategoryAndMessageLength(t *testing.T) {
	oldWebhook := config.DiscordWebhookURL
	defer func() { config.DiscordWebhookURL = oldWebhook }()
	config.DiscordWebhookURL = "https://discord.com/api/webhooks/123/token"
	contactLimiter = newIPRateLimiter(time.Minute, 3, 100)
	ts := setupTestServer()
	defer ts.Close()

	cases := []string{
		`{"type":"unknown","message":"Hello"}`,
		`{"type":"feedback","message":"   "}`,
		`{"type":"feedback","message":"` + strings.Repeat("x", 1501) + `"}`,
	}
	for _, body := range cases {
		res := postContact(ts, body, "")
		res.Body.Close()
		if res.StatusCode != http.StatusUnprocessableEntity {
			t.Errorf("body validation status = %d, want 422", res.StatusCode)
		}
	}
}

(function () {
    "use strict";
    const app = document.getElementById("app");
    const toast = document.getElementById("toast");
    const userIdInput = document.getElementById("userIdInput");
    const SLOT_LABELS = {
        mealTime: "Meal time",
        mood: "Mood",
        scene: "Setting",
        healthGoal: "Health goal",
        cuisine: "Cuisine",
        taste: "Taste",
        convenience: "Convenience"
    };
    const INTENTS = [
        "MEAL_RECOMMENDATION",
        "CLARIFY_NEEDED",
        "MEAL_ADJUST",
        "MEAL_PLAN",
        "HEALTH_RISK",
        "OTHER"
    ];
    const state = {
        home: { loaded: false, personalCount: 0, publicCount: 0 },
        slotOptions: null,
        personalMeals: [],
        publicMeals: [],
        editingMeal: null,
        chat: {
            sourceMode: "PERSONAL",
            sessionId: null,
            sending: false,
            messages: [
                {
                    role: "assistant",
                    text: "Hi! I can recommend meals from your personal collection or the public collection. Try asking: What is a light dinner I can make tonight?"
                }
            ]
        },
        traces: {
            rows: [],
            selected: null,
            loading: false,
            filters: defaultTraceFilters()
        },
        evaluation: {
            report: null,
            loading: false,
            form: defaultRangeForm()
        }
    };
    function defaultRangeForm() {
        const end = new Date();
        const start = new Date(end.getTime() - 24 * 60 * 60 * 1000);
        return {
            startAt: toLocalInputValue(start),
            endAt: toLocalInputValue(end),
            limit: 50,
            includeLlmJudge: false
        };
    }
    function defaultTraceFilters() {
        const range = defaultRangeForm();
        return {
            startAt: range.startAt,
            endAt: range.endAt,
            onlyUnlabeled: false,
            limit: 50,
            sessionId: ""
        };
    }
    function toLocalInputValue(date) {
        const pad = (value) => String(value).padStart(2, "0");
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
    }
    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }
    function safeJson(value) {
        if (value === null || value === undefined || value === "") {
            return "";
        }
        try {
            const parsed = typeof value === "string" ? JSON.parse(value) : value;
            return JSON.stringify(parsed, null, 2);
        } catch (error) {
            return String(value);
        }
    }
    function showToast(message, type) {
        toast.textContent = message;
        toast.className = `toast show ${type === "error" ? "error" : ""}`;
        window.clearTimeout(showToast.timer);
        showToast.timer = window.setTimeout(() => {
            toast.className = "toast";
        }, 3200);
    }
    function setLoading(button, loadingText) {
        if (!button) {
            return () => {};
        }
        const oldText = button.textContent;
        button.disabled = true;
        button.textContent = loadingText || "Working...";
        return () => {
            button.disabled = false;
            button.textContent = oldText;
        };
    }
    async function guard(action, successMessage) {
        try {
            const result = await action();
            if (successMessage) {
                showToast(successMessage);
            }
            return result;
        } catch (error) {
            showToast(error.message || "Something went wrong.", "error");
            throw error;
        }
    }
    function currentRoute() {
        return (location.hash || "#/diet").slice(1).split("?")[0] || "/diet";
    }
    function navigate(route) {
        location.hash = route;
    }
    function setActiveNav(route) {
        document.querySelectorAll("[data-nav]").forEach((item) => {
            item.classList.toggle("active", item.dataset.nav === route);
        });
    }
    function render() {
        const route = currentRoute();
        setActiveNav(route);
        if (route === "/diet") {
            renderHome();
        } else if (route === "/diet/chat") {
            renderChat();
        } else if (route === "/diet/meals/personal") {
            renderPersonalMeals();
        } else if (route === "/diet/meals/public") {
            renderPublicMeals();
        } else if (route === "/admin/traces") {
            renderTraces();
        } else if (route === "/admin/evaluations") {
            renderEvaluations();
        } else {
            navigate("/diet");
        }
        app.focus({ preventScroll: true });
    }
    function renderHome() {
        app.innerHTML = `
            <section class="hero">
                <div class="hero-panel">
                    <span class="badge">Multi-agent meal recommendations</span>
                    <h1>Make today’s meal choice a little easier</h1>
                    <p>Build a personal meal collection or start with public meals. The assistant considers meal time, mood, setting, health goals, taste, and convenience, then asks a follow-up when needed.</p>
                    <div class="hero-actions">
                        <a class="btn primary" href="#/diet/chat">Start a recommendation chat</a>
                        <a class="btn soft" href="#/diet/meals/personal">Manage my meals</a>
                        <a class="btn ghost" href="#/admin/traces">View traces</a>
                    </div>
                </div>
                <aside class="grid stats">
                    ${statCard("My meals", state.home.loaded ? state.home.personalCount : "Loading", "Your private meal collection for personalized recommendations")}
                    ${statCard("Public meals", state.home.loaded ? state.home.publicCount : "Loading", "Built-in meals for getting started quickly")}
                    ${statCard("Current user", DietApi.getUserId(), "Every request includes X-User-Id")}
                </aside>
            </section>
            <section class="grid three" style="margin-top: 18px;">
                ${featureCard("Recommendation chat", "Describe what you want naturally; the app shows follow-up questions, meal cards, and feedback actions.", "#/diet/chat")}
                ${featureCard("Meal management", "Tag the meals you often eat so recommendations can search your own collection first.", "#/diet/meals/personal")}
                ${featureCard("Evaluation", "Review request traces, add expected outcomes, and generate batch evaluation reports.", "#/admin/evaluations")}
            </section>
        `;
        loadHomeStats();
    }
    function statCard(label, value, desc) {
        return `
            <div class="stat-card">
                <span class="muted">${escapeHtml(label)}</span>
                <strong>${escapeHtml(value)}</strong>
                <p class="muted">${escapeHtml(desc)}</p>
            </div>
        `;
    }
    function featureCard(title, desc, href) {
        return `
            <article class="card">
                <div class="card-title">
                    <div>
                        <h3>${escapeHtml(title)}</h3>
                        <p>${escapeHtml(desc)}</p>
                    </div>
                </div>
                <a class="btn soft" href="${href}">Open</a>
            </article>
        `;
    }
    async function loadHomeStats() {
        if (state.home.loaded) {
            return;
        }
        try {
            const [personal, publicMeals] = await Promise.all([
                DietApi.listPersonalMeals(),
                DietApi.listPublicMeals()
            ]);
            state.home = {
                loaded: true,
                personalCount: personal.length,
                publicCount: publicMeals.length
            };
            if (currentRoute() === "/diet") {
                renderHome();
            }
        } catch (error) {
            showToast(error.message || "Could not load the home page data.", "error");
        }
    }
    function renderChat() {
        app.innerHTML = `
            <section class="chat-layout">
                <div class="section chat-window">
                    <div class="card-title">
                        <div>
                            <h2>Recommendation chat</h2>
                            <p>Current session: ${state.chat.sessionId ? escapeHtml(state.chat.sessionId) : "created automatically when you send a message"}</p>
                        </div>
                        <div class="inline-actions">
                            <button class="btn ${state.chat.sourceMode === "PERSONAL" ? "soft" : "ghost"}" data-action="set-source" data-source="PERSONAL">My meals</button>
                            <button class="btn ${state.chat.sourceMode === "PUBLIC" ? "soft" : "ghost"}" data-action="set-source" data-source="PUBLIC">Public meals</button>
                            <button class="btn ghost" data-action="new-session">New chat</button>
                        </div>
                    </div>
                    <div id="messages" class="messages">${state.chat.messages.map(renderMessage).join("")}</div>
                    <form id="chatForm" class="composer">
                        <textarea name="message" placeholder="For example: I want a quick, light dinner tonight." required></textarea>
                        <button class="btn primary" type="submit">${state.chat.sending ? "Sending..." : "Send"}</button>
                    </form>
                </div>
                <aside class="grid">
                    <div class="card">
                        <div class="card-title">
                            <div>
                                <h3>Quick prompts</h3>
                                <p>Select one to add it to the message box.</p>
                            </div>
                        </div>
                        <div class="chips">
                            ${["I need a convenient breakfast.", "Recommend a light, lower-fat dinner.", "I feel a little low today and want something warm.", "Show me another set of options.", "I have an upset stomach—what general meal options are gentle?"].map((text) => `<button class="chip" data-action="quick-message" data-message="${escapeHtml(text)}">${escapeHtml(text)}</button>`).join("")}
                        </div>
                    </div>
                    <div class="card">
                        <h3>Helpful tip</h3>
                        <p class="muted">MY MEALS uses your personal collection. If it is empty, add meals first or switch to PUBLIC MEALS to try the assistant.</p>
                        <div class="button-row">
                            <a class="btn soft" href="#/diet/meals/personal">Manage meals</a>
                            <a class="btn ghost" href="#/diet/meals/public">View public meals</a>
                        </div>
                    </div>
                </aside>
            </section>
        `;
        scrollMessagesToBottom();
    }
    function renderMessage(message) {
        const mealCards = (message.meals || []).map((meal) => renderMealCard(meal, { feedback: true, sessionId: message.sessionId })).join("");
        const missingSlots = message.missingSlots && message.missingSlots.length
            ? `<div class="chips">${message.missingSlots.map((slot) => `<span class="chip selected">${escapeHtml(SLOT_LABELS[slot] || slot)}</span>`).join("")}</div>`
            : "";
        const trace = message.traceId
            ? `<span>traceId：<a href="#/admin/traces" data-action="open-trace" data-trace-id="${escapeHtml(message.traceId)}">${escapeHtml(message.traceId)}</a></span>`
            : "";
        return `
            <article class="message ${message.role}">
                <div class="bubble">${escapeHtml(message.text)}</div>
                ${missingSlots}
                ${mealCards ? `<div class="grid">${mealCards}</div>` : ""}
                ${trace ? `<div class="message-meta">${trace}</div>` : ""}
            </article>
        `;
    }
    function scrollMessagesToBottom() {
        const messages = document.getElementById("messages");
        if (messages) {
            messages.scrollTop = messages.scrollHeight;
        }
    }
    async function submitChat(form) {
        const messageInput = form.elements.message;
        const message = messageInput.value.trim();
        if (!message || state.chat.sending) {
            return;
        }
        state.chat.messages.push({ role: "user", text: message });
        messageInput.value = "";
        state.chat.sending = true;
        renderChat();
        try {
            if (!state.chat.sessionId) {
                const session = await DietApi.createSession();
                state.chat.sessionId = session.sessionId;
            }
            const response = await DietApi.chat({
                sessionId: state.chat.sessionId,
                message,
                sourceMode: state.chat.sourceMode,
                context: {}
            });
            state.chat.sessionId = response.sessionId || state.chat.sessionId;
            state.chat.messages.push({
                role: "assistant",
                text: response.clarifyQuestion || response.speechText || "Your request is complete.",
                responseType: response.responseType,
                meals: response.displayBlocks || [],
                missingSlots: response.missingSlots || [],
                traceId: response.traceId,
                sessionId: response.sessionId || state.chat.sessionId
            });
        } catch (error) {
            showToast(error.message || "The chat request failed.", "error");
            state.chat.messages.push({ role: "assistant", text: "That request did not complete. Please try again shortly." });
        } finally {
            state.chat.sending = false;
            renderChat();
        }
    }
    function resetChat() {
        state.chat.sessionId = null;
        state.chat.messages = [
            {
                role: "assistant",
                text: "New chat started. Tell me the meal time, taste, setting, or health goal you have in mind."
            }
        ];
        renderChat();
    }
    async function renderPersonalMeals() {
        if (!state.slotOptions) {
            app.innerHTML = `<section class="section"><div class="empty">Loading meal tags...</div></section>`;
            await ensureSlotOptions();
            if (currentRoute() !== "/diet/meals/personal") {
                return;
            }
        }
        await ensurePersonalMeals();
        if (currentRoute() !== "/diet/meals/personal") {
            return;
        }
        app.innerHTML = `
            <section class="split">
                <div class="section">
                    <div class="card-title">
                        <div>
                            <h2>My meals</h2>
                            <p>Manage meals you often eat, then switch to My meals in the recommendation chat.</p>
                        </div>
                        <button class="btn primary" data-action="new-meal">Add meal</button>
                    </div>
                    <div id="personalMealList">${renderMealList(state.personalMeals, { editable: true })}</div>
                </div>
                <aside class="section">
                    ${renderMealForm()}
                </aside>
            </section>
        `;
    }
    function renderMealForm() {
        const meal = state.editingMeal || emptyMeal();
        const title = meal.id ? "Edit meal" : "Add meal";
        return `
            <div class="card-title">
                <div>
                    <h3>${title}</h3>
                    <p>Select tags from the lists below. Meal time is required; the other fields are optional.</p>
                </div>
            </div>
            <form id="mealForm" class="form-grid">
                <input type="hidden" name="mealId" value="${escapeHtml(meal.id || "")}">
                <div class="field full">
                    <label for="mealName">Meal name</label>
                    <input id="mealName" name="name" value="${escapeHtml(meal.name || "")}" placeholder="For example: Tomato and egg noodles" required>
                </div>
                <p class="field-hint full">Tag lists support multiple choices: hold Ctrl on Windows or Command on Mac while selecting.</p>
                ${Object.entries(SLOT_LABELS).map(([key, label]) => renderSlotPicker(key, label, meal[key] || [])).join("")}
                <div class="field full">
                    <div class="button-row">
                        <button class="btn primary" type="submit">${meal.id ? "Save changes" : "Create meal"}</button>
                        <button class="btn ghost" type="button" data-action="cancel-edit">Clear</button>
                    </div>
                </div>
            </form>
        `;
    }
    function renderSlotPicker(key, label, selected) {
        const options = state.slotOptions && state.slotOptions[key] ? state.slotOptions[key] : [];
        const selectedSet = new Set(selected || []);
        const required = key === "mealTime";
        return `
            <div class="field">
                <label for="slot-${escapeHtml(key)}">${escapeHtml(label)}${required ? " (required)" : ""}</label>
                <select
                    id="slot-${escapeHtml(key)}"
                    class="slot-select"
                    name="${escapeHtml(key)}"
                    multiple
                    size="5"
                    ${required ? "required" : ""}
                >
                    ${options.map((option) => {
                        const isSelected = selectedSet.has(option);
                        return `<option value="${escapeHtml(option)}" ${isSelected ? "selected" : ""}>${escapeHtml(option)}</option>`;
                    }).join("")}
                </select>
            </div>
        `;
    }
    function emptyMeal() {
        return {
            name: "",
            mealTime: [],
            mood: [],
            scene: [],
            healthGoal: [],
            cuisine: [],
            taste: [],
            convenience: []
        };
    }
    function renderMealList(meals, options) {
        if (!meals.length) {
            return `<div class="empty">No meals yet. Add a few meals you often eat to get started.</div>`;
        }
        return `<div class="grid two">${meals.map((meal) => renderMealCard(meal, options || {})).join("")}</div>`;
    }
    function renderMealCard(meal, options) {
        const editable = options && options.editable;
        const feedback = options && options.feedback;
        return `
            <article class="meal-card">
                <header>
                    <div>
                        <h3>${escapeHtml(meal.name)}</h3>
                        <p class="muted">${escapeHtml(meal.sourceType || "")}</p>
                    </div>
                    ${meal.matchScore ? `<span class="score">${Math.round(meal.matchScore * 100)}% match</span>` : ""}
                </header>
                <div class="chips">${mealTags(meal).map((tag) => `<span class="chip selected">${escapeHtml(tag)}</span>`).join("")}</div>
                ${editable ? `
                    <div class="button-row">
                        <button class="btn soft" data-action="edit-meal" data-id="${escapeHtml(meal.id)}">Edit</button>
                        <button class="btn ghost" data-action="delete-meal" data-id="${escapeHtml(meal.id)}">Delete</button>
                    </div>
                ` : ""}
                ${feedback ? `
                    <div class="button-row">
                        <button class="btn soft" data-action="feedback" data-action-value="LIKE" data-item-id="${escapeHtml(meal.id)}" data-session-id="${escapeHtml(options.sessionId || "")}">Like</button>
                        <button class="btn ghost" data-action="feedback" data-action-value="ADOPT" data-item-id="${escapeHtml(meal.id)}" data-session-id="${escapeHtml(options.sessionId || "")}">Choose this</button>
                        <button class="btn ghost" data-action="feedback" data-action-value="DISLIKE" data-item-id="${escapeHtml(meal.id)}" data-session-id="${escapeHtml(options.sessionId || "")}">Not for me</button>
                    </div>
                ` : ""}
            </article>
        `;
    }
    function mealTags(meal) {
        return Object.keys(SLOT_LABELS).flatMap((key) => (meal[key] || []).map((value) => `${SLOT_LABELS[key]}：${value}`));
    }
    async function ensurePersonalMeals(force) {
        if (!force && state.personalMeals.length) {
            return;
        }
        try {
            state.personalMeals = await DietApi.listPersonalMeals();
            state.home.loaded = false;
            if (currentRoute() === "/diet/meals/personal") {
                document.getElementById("personalMealList").innerHTML = renderMealList(state.personalMeals, { editable: true });
            }
        } catch (error) {
            showToast(error.message || "Could not load your meals.", "error");
        }
    }
    async function ensureSlotOptions() {
        if (state.slotOptions) {
            return;
        }
        try {
            state.slotOptions = await DietApi.slotOptions();
        } catch (error) {
            showToast(error.message || "Could not load meal tags.", "error");
            throw error;
        }
    }
    async function saveMeal(form) {
        const { id, payload } = mealPayloadFromForm(form);
        if (!payload.name) {
            showToast("Enter a meal name.", "error");
            return;
        }
        if (!payload.mealTime.length) {
            showToast("Select at least one meal-time tag.", "error");
            return;
        }
        const restore = setLoading(form.querySelector("button[type=submit]"), "Saving...");
        try {
            await guard(async () => {
                if (id) {
                    return DietApi.updatePersonalMeal(id, payload);
                }
                return DietApi.createPersonalMeal(payload);
            }, id ? "Meal updated." : "Meal created.");
            state.editingMeal = null;
            await ensurePersonalMeals(true);
            renderPersonalMeals();
        } finally {
            restore();
        }
    }
    function mealPayloadFromForm(form) {
        const formData = new FormData(form);
        const payload = {
            name: String(formData.get("name") || "").trim()
        };
        Object.keys(SLOT_LABELS).forEach((key) => {
            payload[key] = formData.getAll(key).filter(Boolean);
        });
        return {
            id: String(formData.get("mealId") || "").trim(),
            payload
        };
    }
    function editMeal(id) {
        const meal = state.personalMeals.find((item) => String(item.id) === String(id));
        if (!meal) {
            showToast("The meal to edit was not found.", "error");
            return;
        }
        state.editingMeal = JSON.parse(JSON.stringify(meal));
        renderPersonalMeals();
    }
    async function deleteMeal(id) {
        const meal = state.personalMeals.find((item) => String(item.id) === String(id));
        if (!meal || !window.confirm(`Delete “${meal.name}”?`)) {
            return;
        }
        await guard(async () => {
            await DietApi.deletePersonalMeal(id);
            await ensurePersonalMeals(true);
            renderPersonalMeals();
        }, "Meal deleted.");
    }
    function renderPublicMeals() {
        app.innerHTML = `
            <section class="section">
                <div class="card-title">
                    <div>
                            <h2>Public meals</h2>
                            <p>This built-in, read-only collection is available from the chat page when you choose Public meals.</p>
                    </div>
                    <a class="btn primary" href="#/diet/chat">Start a recommendation chat</a>
                </div>
                <div id="publicMealList">${renderMealList(state.publicMeals, {})}</div>
            </section>
        `;
        ensurePublicMeals();
    }
    async function ensurePublicMeals(force) {
        if (!force && state.publicMeals.length) {
            return;
        }
        try {
            state.publicMeals = await DietApi.listPublicMeals();
            state.home.loaded = false;
            if (currentRoute() === "/diet/meals/public") {
                document.getElementById("publicMealList").innerHTML = renderMealList(state.publicMeals, {});
            }
        } catch (error) {
            showToast(error.message || "Could not load public meals.", "error");
        }
    }
    function renderTraces() {
        const selected = state.traces.selected;
        app.innerHTML = `
            <section class="split">
                <div class="section">
                    <div class="card-title">
                        <div>
                            <h2>Trace explorer</h2>
                            <p>Search request traces by time range or session, then review intent revisions, slots, and recommendation events.</p>
                        </div>
                    </div>
                    <form id="traceFilterForm" class="form-grid">
                        <div class="field">
                            <label>Start time</label>
                            <input type="datetime-local" name="startAt" value="${escapeHtml(state.traces.filters.startAt)}" required>
                        </div>
                        <div class="field">
                            <label>End time</label>
                            <input type="datetime-local" name="endAt" value="${escapeHtml(state.traces.filters.endAt)}" required>
                        </div>
                        <div class="field">
                            <label>Session ID (optional)</label>
                            <input name="sessionId" value="${escapeHtml(state.traces.filters.sessionId)}" placeholder="Enter a session ID to search by session">
                        </div>
                        <div class="field">
                            <label>Result limit</label>
                            <input type="number" min="1" max="500" name="limit" value="${escapeHtml(state.traces.filters.limit)}">
                        </div>
                        <div class="field">
                            <label>Label status</label>
                            <select name="onlyUnlabeled">
                                <option value="false" ${!state.traces.filters.onlyUnlabeled ? "selected" : ""}>All</option>
                                <option value="true" ${state.traces.filters.onlyUnlabeled ? "selected" : ""}>Unlabeled only</option>
                            </select>
                        </div>
                        <div class="field">
                            <span>&nbsp;</span>
                            <button class="btn primary" type="submit">${state.traces.loading ? "Searching..." : "Search traces"}</button>
                        </div>
                    </form>
                    <div class="subtle-divider"></div>
                    ${renderTraceTable()}
                </div>
                <aside class="section">
                    ${selected ? renderTraceDetail(selected) : `<div class="empty">Select a trace to view its details and labeling form.</div>`}
                </aside>
            </section>
        `;
    }
    function renderTraceTable() {
        if (!state.traces.rows.length) {
            return `<div class="empty">No trace data yet. Start a few conversations in the chat page first.</div>`;
        }
        return `
            <div class="table-wrap">
                <table>
                    <thead>
                        <tr>
                            <th>Trace ID</th>
                            <th>Session</th>
                            <th>Status</th>
                            <th>Events</th>
                            <th>Duration</th>
                            <th>Created</th>
                            <th>Label</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${state.traces.rows.map((row) => `
                            <tr>
                                <td>${escapeHtml(row.traceId)}</td>
                                <td>${escapeHtml(row.sessionId)}</td>
                                <td>${escapeHtml(row.status || "-")}</td>
                                <td>${escapeHtml(row.eventCount ?? "-")}</td>
                                <td>${row.durationMs ? `${escapeHtml(row.durationMs)} ms` : "-"}</td>
                                <td>${escapeHtml(row.createdAt || "-")}</td>
                                <td>${row.expectedIntent ? `<span class="badge">${escapeHtml(row.expectedIntent)}</span>` : "<span class=\"muted\">Unlabeled</span>"}</td>
                                <td><button class="btn soft" data-action="select-trace" data-trace-id="${escapeHtml(row.traceId)}">View</button></td>
                            </tr>
                        `).join("")}
                    </tbody>
                </table>
            </div>
        `;
    }
    function renderTraceDetail(trace) {
        return `
            <div class="card-title">
                <div>
                    <h3>Trace details</h3>
                    <p>${escapeHtml(trace.traceId)}</p>
                </div>
            </div>
            <div class="grid">
                <div>
                    <span class="badge">${escapeHtml(trace.status || "UNKNOWN")}</span>
                    <p class="muted">Session：${escapeHtml(trace.sessionId || "-")} · Events：${escapeHtml(trace.eventCount ?? "-")} · Duration：${escapeHtml(trace.durationMs ?? "-")} ms</p>
                </div>
                <details open>
                    <summary>Trace JSON</summary>
                    <pre class="json-box">${escapeHtml(safeJson(trace.traceJson))}</pre>
                </details>
                <form id="traceLabelForm" class="form-grid">
                    <input type="hidden" name="traceId" value="${escapeHtml(trace.traceId)}">
                    <div class="field">
                        <label>Expected intent</label>
                        <select name="expectedIntent">
                            <option value="">Do not label</option>
                            ${INTENTS.map((intent) => `<option value="${intent}" ${trace.expectedIntent === intent ? "selected" : ""}>${intent}</option>`).join("")}
                        </select>
                    </div>
                    <div class="field">
                        <label>Clarification action</label>
                        <select name="expectedClarifyAction">
                            <option value="">Do not label</option>
                            <option value="ASK" ${trace.expectedClarifyAction === "ASK" ? "selected" : ""}>ASK</option>
                            <option value="READY" ${trace.expectedClarifyAction === "READY" ? "selected" : ""}>READY</option>
                        </select>
                    </div>
                    <div class="field full">
                        <label>Expected slots JSON</label>
                        <textarea name="expectedSlots" placeholder='{"mealTime":["Dinner"],"taste":["Mild"]}'>${escapeHtml(safeJson(trace.expectedSlots))}</textarea>
                    </div>
                    <div class="field full">
                        <label>Note</label>
                        <textarea name="labelNote" placeholder="Labeling note">${escapeHtml(trace.labelNote || "")}</textarea>
                    </div>
                    <div class="field full">
                        <button class="btn primary" type="submit">Save label</button>
                    </div>
                </form>
            </div>
        `;
    }
    async function searchTraces(form) {
        const formData = new FormData(form);
        state.traces.filters = {
            startAt: formData.get("startAt"),
            endAt: formData.get("endAt"),
            sessionId: formData.get("sessionId").trim(),
            onlyUnlabeled: formData.get("onlyUnlabeled") === "true",
            limit: Number(formData.get("limit") || 50)
        };
        state.traces.loading = true;
        renderTraces();
        try {
            if (state.traces.filters.sessionId) {
                state.traces.rows = await DietApi.listSessionTraces(state.traces.filters.sessionId, state.traces.filters.limit);
            } else {
                state.traces.rows = await DietApi.listTraces({
                    startAt: state.traces.filters.startAt,
                    endAt: state.traces.filters.endAt,
                    onlyUnlabeled: state.traces.filters.onlyUnlabeled,
                    limit: state.traces.filters.limit
                });
            }
            state.traces.selected = state.traces.rows[0] || null;
        } catch (error) {
            showToast(error.message || "Could not search traces.", "error");
        } finally {
            state.traces.loading = false;
            renderTraces();
        }
    }
    async function selectTrace(traceId) {
        await guard(async () => {
            state.traces.selected = await DietApi.getTrace(traceId);
            renderTraces();
        });
    }
    async function saveTraceLabel(form) {
        const formData = new FormData(form);
        const traceId = formData.get("traceId");
        const slotsText = formData.get("expectedSlots").trim();
        let expectedSlots = null;
        if (slotsText) {
            try {
                expectedSlots = JSON.parse(slotsText);
            } catch (error) {
                showToast("Expected slots must be valid JSON.", "error");
                return;
            }
        }
        const payload = {
            expectedIntent: formData.get("expectedIntent") || null,
            expectedSlots,
            expectedClarifyAction: formData.get("expectedClarifyAction") || null,
            labelNote: formData.get("labelNote").trim()
        };
        await guard(async () => {
            await DietApi.labelTrace(traceId, payload);
            state.traces.selected = await DietApi.getTrace(traceId);
            const index = state.traces.rows.findIndex((row) => row.traceId === traceId);
            if (index >= 0) {
                state.traces.rows[index] = state.traces.selected;
            }
            renderTraces();
        }, "Trace label saved.");
    }
    function renderEvaluations() {
        app.innerHTML = `
            <section class="section">
                <div class="card-title">
                    <div>
                            <h2>Evaluation report</h2>
                            <p>Generate rule scores, optional LLM-judge scores, and feedback attribution from stored traces.</p>
                    </div>
                </div>
                <form id="evaluationForm" class="form-grid">
                    <div class="field">
                        <label>Start time</label>
                        <input type="datetime-local" name="startAt" value="${escapeHtml(state.evaluation.form.startAt)}" required>
                    </div>
                    <div class="field">
                        <label>End time</label>
                        <input type="datetime-local" name="endAt" value="${escapeHtml(state.evaluation.form.endAt)}" required>
                    </div>
                    <div class="field">
                        <label>Result limit</label>
                        <input type="number" min="1" max="500" name="limit" value="${escapeHtml(state.evaluation.form.limit)}">
                    </div>
                    <div class="field">
                        <label>LLM Judge</label>
                        <select name="includeLlmJudge">
                            <option value="false" ${!state.evaluation.form.includeLlmJudge ? "selected" : ""}>Off</option>
                            <option value="true" ${state.evaluation.form.includeLlmJudge ? "selected" : ""}>On</option>
                        </select>
                    </div>
                    <div class="field full">
                        <button class="btn primary" type="submit">${state.evaluation.loading ? "Evaluating..." : "Generate report"}</button>
                    </div>
                </form>
            </section>
            <section class="section" style="margin-top: 18px;">
                ${renderEvaluationReport()}
            </section>
        `;
    }
    function renderEvaluationReport() {
        const report = state.evaluation.report;
        if (!report) {
            return `<div class="empty">No report yet. Select a time range and generate one.</div>`;
        }
        return `
            <div class="grid three">
                ${statCard("Total traces", report.totalTraces, "Requests included in this evaluation")}
                ${statCard("Labeled", report.labeledTraces, "Traces with a human label")}
                ${statCard("Average score", report.avgScore === null || report.avgScore === undefined ? "-" : Number(report.avgScore).toFixed(2), "Combined score")}
            </div>
            <div class="subtle-divider"></div>
            <div class="grid two">
                <div>
                    <h3>Metric averages</h3>
                    ${renderMetrics(report.metricAverages)}
                </div>
                <div>
                    <h3>Report range</h3>
                    <p class="muted">${escapeHtml(report.startAt)} to ${escapeHtml(report.endAt)}</p>
                </div>
            </div>
            <div class="subtle-divider"></div>
            ${renderEvaluationTable(report.traceResults || [])}
        `;
    }
    function renderMetrics(metrics) {
        const entries = Object.entries(metrics || {});
        if (!entries.length) {
            return `<div class="empty">No metrics available.</div>`;
        }
        return `<div class="chips">${entries.map(([key, value]) => `<span class="chip selected">${escapeHtml(key)}：${Number(value).toFixed(2)}</span>`).join("")}</div>`;
    }
    function renderEvaluationTable(rows) {
        if (!rows.length) {
            return `<div class="empty">No trace details available.</div>`;
        }
        return `
            <div class="table-wrap">
                <table>
                    <thead>
                        <tr>
                            <th>Trace ID</th>
                            <th>Session</th>
                            <th>Combined score</th>
                            <th>Rule score</th>
                            <th>LLM score</th>
                            <th>Feedback score</th>
                            <th>Metrics / details</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${rows.map((row) => `
                            <tr>
                                <td>${escapeHtml(row.traceId)}</td>
                                <td>${escapeHtml(row.sessionId)}</td>
                                <td>${formatScore(row.score)}</td>
                                <td>${formatScore(row.ruleScore)}</td>
                                <td>${formatScore(row.llmJudgeScore)}</td>
                                <td>${formatScore(row.userFeedbackScore)}</td>
                                <td>
                                    <details>
                                        <summary>View JSON</summary>
                                        <pre class="json-box">${escapeHtml(JSON.stringify({ metrics: row.metrics, detail: row.detail }, null, 2))}</pre>
                                    </details>
                                </td>
                            </tr>
                        `).join("")}
                    </tbody>
                </table>
            </div>
        `;
    }
    function formatScore(value) {
        return value === null || value === undefined ? "-" : Number(value).toFixed(2);
    }
    async function runEvaluation(form) {
        const formData = new FormData(form);
        state.evaluation.form = {
            startAt: formData.get("startAt"),
            endAt: formData.get("endAt"),
            limit: Number(formData.get("limit") || 50),
            includeLlmJudge: formData.get("includeLlmJudge") === "true"
        };
        state.evaluation.loading = true;
        renderEvaluations();
        try {
            state.evaluation.report = await DietApi.evaluate(state.evaluation.form);
        } catch (error) {
            showToast(error.message || "Evaluation failed.", "error");
        } finally {
            state.evaluation.loading = false;
            renderEvaluations();
        }
    }
    async function saveFeedback(button) {
        await guard(async () => {
            await DietApi.saveFeedback({
                sessionId: button.dataset.sessionId || state.chat.sessionId,
                itemId: Number(button.dataset.itemId),
                action: button.dataset.actionValue,
                rating: button.dataset.actionValue === "DISLIKE" ? 2 : 5,
                reason: ""
            });
        }, "Feedback saved.");
    }
    function handleClick(event) {
        const target = event.target.closest("[data-action]");
        if (!target) {
            return;
        }
        const action = target.dataset.action;
        if (action === "set-source") {
            state.chat.sourceMode = target.dataset.source;
            resetChat();
        } else if (action === "new-session") {
            resetChat();
        } else if (action === "quick-message") {
            const input = document.querySelector("#chatForm textarea[name=message]");
            if (input) {
                input.value = target.dataset.message;
                input.focus();
            }
        } else if (action === "feedback") {
            saveFeedback(target);
        } else if (action === "new-meal") {
            state.editingMeal = emptyMeal();
            renderPersonalMeals();
        } else if (action === "edit-meal") {
            editMeal(target.dataset.id);
        } else if (action === "delete-meal") {
            deleteMeal(target.dataset.id);
        } else if (action === "cancel-edit") {
            state.editingMeal = null;
            renderPersonalMeals();
        } else if (action === "select-trace") {
            selectTrace(target.dataset.traceId);
        } else if (action === "open-trace") {
            state.traces.filters.sessionId = "";
            navigate("/admin/traces");
            selectTrace(target.dataset.traceId);
        }
    }
    function handleSubmit(event) {
        const form = event.target;
        if (form.id === "chatForm") {
            event.preventDefault();
            submitChat(form);
        } else if (form.id === "mealForm") {
            event.preventDefault();
            if (!form.checkValidity()) {
                form.reportValidity();
                return;
            }
            saveMeal(form);
        } else if (form.id === "traceFilterForm") {
            event.preventDefault();
            searchTraces(form);
        } else if (form.id === "traceLabelForm") {
            event.preventDefault();
            saveTraceLabel(form);
        } else if (form.id === "evaluationForm") {
            event.preventDefault();
            runEvaluation(form);
        }
    }
    function initUserField() {
        userIdInput.value = DietApi.setUserId(DietApi.getUserId());
        userIdInput.addEventListener("change", () => {
            DietApi.setUserId(userIdInput.value);
            state.home.loaded = false;
            state.personalMeals = [];
            state.publicMeals = [];
            state.traces.rows = [];
            state.traces.selected = null;
            resetChat();
            showToast("User ID switched.");
            render();
        });
    }
    window.addEventListener("hashchange", render);
    app.addEventListener("click", handleClick);
    app.addEventListener("submit", handleSubmit);
    initUserField();
    if (!location.hash) {
        navigate("/diet");
    } else {
        render();
    }
})();

"use strict";

const STATUS_LABELS = Object.freeze({
    PENDING: "待处理",
    PROCESSING: "处理中",
    WAITING_CHECK: "待验收",
    COMPLETED: "已完成",
    IN_PROGRESS: "进行中",
    SKIPPED: "已跳过",
});

const PRIORITY_LABELS = Object.freeze({ URGENT: "紧急", NORMAL: "普通", LOW: "低" });
const TYPE_LABELS = Object.freeze({
    PLUMBING: "水电维修",
    FURNITURE: "家具维修",
    APPLIANCE: "电器维修",
    NETWORK: "网络维修",
    OTHER: "其他",
});
const TAB_IDS = new Set(["repairs", "cleaning"]);
const REPAIR_STATUS_VALUES = new Set(["PENDING", "PROCESSING", "WAITING_CHECK", "COMPLETED"]);
const REPAIR_TYPE_VALUES = new Set(Object.keys(TYPE_LABELS));

const state = {
    page: 1,
    size: 10,
    total: 0,
    status: "",
    type: "",
    query: "",
    activeTab: "repairs",
    dashboard: null,
    repairs: [],
    dashboardHealthy: null,
    repairHealthy: null,
    loadedRepairPage: 1,
    dashboardSequence: 0,
    repairSequence: 0,
    dialogSubmit: null,
};

const elements = {};
let searchTimer;
let toastTimer;
let dialogReturnFocus;
let dashboardAbortController;
let repairAbortController;

function byId(id) {
    return document.getElementById(id);
}

function init() {
    Object.assign(elements, {
        sidebar: byId("sidebar"),
        sidebarScrim: byId("sidebar-scrim"),
        menuButton: byId("menu-button"),
        refreshButton: byId("refresh-button"),
        metrics: document.querySelector(".metrics"),
        systemState: byId("system-state"),
        systemStateLabel: byId("system-state-label"),
        repairPanel: byId("repairs"),
        filterSummary: byId("filter-summary"),
        filterSummaryText: byId("filter-summary-text"),
        clearFiltersButton: byId("clear-filters"),
        emptyClearFiltersButton: byId("empty-clear-filters"),
        repairSyncNote: byId("repair-sync-note"),
        repairTableBody: byId("repair-table-body"),
        repairMobileList: byId("repair-mobile-list"),
        repairEmpty: byId("repair-empty"),
        cleaningList: byId("cleaning-list"),
        cleaningEmpty: byId("cleaning-empty"),
        dialog: byId("action-dialog"),
        dialogForm: byId("action-form"),
        dialogFields: byId("dialog-fields"),
        dialogSubmit: byId("dialog-submit"),
        toast: byId("toast"),
    });

    bindNavigation();
    bindFilters();
    bindActions();
    syncRepairStateFromLocation();
    writeRepairStateToLocation("replace");
    syncNavigationFromLocation();
    updateTodayLabel(new Date());
    setRepairLoading(true);
    refreshAll({ announce: false });
}

function bindNavigation() {
    elements.menuButton.addEventListener("click", () => setSidebarOpen(!elements.sidebar.classList.contains("is-open")));
    elements.sidebarScrim.addEventListener("click", () => setSidebarOpen(false));
    document.querySelector(".brand").addEventListener("click", () => setSidebarOpen(false));
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && elements.sidebar.classList.contains("is-open")) {
            setSidebarOpen(false, true);
        }
    });

    document.querySelectorAll("[data-nav]").forEach((link) => {
        link.addEventListener("click", () => {
            const destination = link.dataset.nav;
            setNavigationDestination(destination);
            if (TAB_IDS.has(destination)) {
                setActiveTab(destination);
            }
            setSidebarOpen(false);
        });
    });

    const tabs = [...document.querySelectorAll("[data-tab]")];
    tabs.forEach((button) => {
        button.addEventListener("click", () => navigateToTab(button.dataset.tab));
    });
    document.querySelector(".mobile-tabs").addEventListener("keydown", (event) => {
        const currentIndex = tabs.indexOf(document.activeElement);
        if (currentIndex < 0) return;
        let nextIndex;
        if (event.key === "ArrowRight") nextIndex = (currentIndex + 1) % tabs.length;
        if (event.key === "ArrowLeft") nextIndex = (currentIndex - 1 + tabs.length) % tabs.length;
        if (event.key === "Home") nextIndex = 0;
        if (event.key === "End") nextIndex = tabs.length - 1;
        if (nextIndex === undefined) return;
        event.preventDefault();
        navigateToTab(tabs[nextIndex].dataset.tab);
        tabs[nextIndex].focus();
    });
    window.addEventListener("hashchange", syncNavigationFromLocation);
    window.addEventListener("popstate", () => {
        syncNavigationFromLocation();
        if (syncRepairStateFromLocation()) loadRepairPage();
    });
}

function bindFilters() {
    byId("status-filter").addEventListener("change", (event) => {
        state.status = event.target.value;
        state.page = 1;
        updateClearFiltersButton();
        writeRepairStateToLocation("push");
        loadRepairPage();
    });
    byId("type-filter").addEventListener("change", (event) => {
        state.type = event.target.value;
        state.page = 1;
        updateClearFiltersButton();
        writeRepairStateToLocation("push");
        loadRepairPage();
    });
    byId("query-filter").addEventListener("input", (event) => {
        window.clearTimeout(searchTimer);
        updateClearFiltersButton();
        searchTimer = window.setTimeout(() => {
            state.query = event.target.value.trim();
            state.page = 1;
            writeRepairStateToLocation("replace");
            loadRepairPage();
        }, 260);
    });
    elements.clearFiltersButton.addEventListener("click", clearRepairFilters);
    elements.emptyClearFiltersButton.addEventListener("click", clearRepairFilters);
    byId("repair-filters").addEventListener("submit", (event) => event.preventDefault());
    byId("previous-page").addEventListener("click", () => {
        if (state.page <= 1) return;
        state.page -= 1;
        writeRepairStateToLocation("push");
        loadRepairPage();
    });
    byId("next-page").addEventListener("click", () => {
        if (state.page * state.size >= state.total) return;
        state.page += 1;
        writeRepairStateToLocation("push");
        loadRepairPage();
    });
}

function syncRepairStateFromLocation() {
    window.clearTimeout(searchTimer);
    const params = new URLSearchParams(window.location.search);
    const rawStatus = params.get("status") || "";
    const rawType = params.get("type") || "";
    const rawQuery = (params.get("q") || "").trim().slice(0, 100);
    const rawPage = params.get("page") || "";
    const parsedPage = /^\d+$/.test(rawPage) ? Number(rawPage) : 1;
    const next = {
        status: REPAIR_STATUS_VALUES.has(rawStatus) ? rawStatus : "",
        type: REPAIR_TYPE_VALUES.has(rawType) ? rawType : "",
        query: rawQuery,
        page: Number.isSafeInteger(parsedPage) && parsedPage > 0 ? parsedPage : 1,
    };
    const changed = next.status !== state.status
        || next.type !== state.type
        || next.query !== state.query
        || next.page !== state.page;
    Object.assign(state, next);
    byId("status-filter").value = state.status;
    byId("type-filter").value = state.type;
    byId("query-filter").value = state.query;
    updateClearFiltersButton();
    return changed;
}

function writeRepairStateToLocation(mode) {
    const url = new URL(window.location.href);
    [["status", state.status], ["type", state.type], ["q", state.query]].forEach(([key, value]) => {
        if (value) {
            url.searchParams.set(key, value);
        } else {
            url.searchParams.delete(key);
        }
    });
    if (state.page > 1) {
        url.searchParams.set("page", String(state.page));
    } else {
        url.searchParams.delete("page");
    }
    const next = `${url.pathname}${url.search}${url.hash}`;
    const current = `${window.location.pathname}${window.location.search}${window.location.hash}`;
    if (next === current) return;
    if (mode === "push") {
        window.history.pushState(null, "", next);
    } else {
        window.history.replaceState(null, "", next);
    }
}

function bindActions() {
    elements.refreshButton.addEventListener("click", () => refreshAll({ announce: true }));
    byId("new-repair-button").addEventListener("click", openNewRepairDialog);
    byId("empty-new-repair").addEventListener("click", openNewRepairDialog);
    byId("new-cleaning-button").addEventListener("click", openNewCleaningDialog);
    byId("dialog-close").addEventListener("click", closeDialog);
    byId("dialog-cancel").addEventListener("click", closeDialog);
    elements.dialog.addEventListener("cancel", (event) => {
        if (elements.dialogForm.getAttribute("aria-busy") === "true") {
            event.preventDefault();
        } else {
            closeDialog();
        }
    });
    byId("toast-close").addEventListener("click", hideToast);

    elements.dialogForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!state.dialogSubmit || !elements.dialogForm.reportValidity()) return;
        const formData = new FormData(elements.dialogForm);
        setDialogLoading(true);
        try {
            await state.dialogSubmit(formData);
            closeDialog();
            await refreshAll({ announce: false });
            showToast("操作成功");
        } catch (error) {
            showToast(error.message || "操作失败，请稍后重试", true);
        } finally {
            setDialogLoading(false);
        }
    });

    const handleRepairAction = (event) => {
        const button = event.target.closest("[data-repair-action]");
        if (!button) return;
        const order = state.repairs.find((item) => item.id === Number(button.dataset.orderId));
        if (order) openRepairActionDialog(order, button.dataset.repairAction);
    };
    elements.repairTableBody.addEventListener("click", handleRepairAction);
    elements.repairMobileList.addEventListener("click", handleRepairAction);

    elements.cleaningList.addEventListener("click", (event) => {
        const button = event.target.closest("[data-cleaning-action]");
        if (!button || !state.dashboard) return;
        const plan = state.dashboard.todayCleaningPlans.find((item) => item.id === Number(button.dataset.planId));
        if (plan) openCleaningActionDialog(plan, button.dataset.cleaningAction);
    });
}

async function refreshAll({ announce }) {
    const sequence = ++state.dashboardSequence;
    dashboardAbortController?.abort();
    const controller = new AbortController();
    dashboardAbortController = controller;
    elements.refreshButton.disabled = true;
    elements.refreshButton.setAttribute("aria-busy", "true");
    byId("refresh-label").textContent = "正在刷新";
    setSystemState("loading");
    try {
        const summary = await requestJson("/api/dashboard/summary", { signal: controller.signal });
        if (sequence !== state.dashboardSequence) return;
        state.dashboard = summary;
        state.dashboardHealthy = true;
        renderDashboard(summary);
        let repairLoaded = true;
        if (hasRepairFilters() || state.page !== 1) {
            repairLoaded = await loadRepairPage();
        } else {
            cancelRepairLoad();
            state.repairs = summary.recentRepairs;
            state.total = summary.totalRepairs;
            state.page = 1;
            state.loadedRepairPage = 1;
            state.repairHealthy = true;
            renderRepairs();
            setRepairLoading(false);
            setRepairStale(false);
            syncSystemState();
        }
        if (announce && repairLoaded) showToast("数据已刷新");
    } catch (error) {
        if (isAbortError(error)) return;
        if (sequence !== state.dashboardSequence) return;
        showToast(error.message || "无法加载运维数据", true);
        state.dashboardHealthy = false;
        if (!state.dashboard) renderLoadFailure();
        setRepairStale(true);
        syncSystemState();
    } finally {
        if (sequence === state.dashboardSequence) {
            if (dashboardAbortController === controller) dashboardAbortController = undefined;
            elements.refreshButton.disabled = false;
            elements.refreshButton.removeAttribute("aria-busy");
            byId("refresh-label").textContent = "刷新";
            elements.metrics.setAttribute("aria-busy", "false");
            if (!hasRepairFilters() || !state.dashboard) setRepairLoading(false);
        }
    }
}

async function loadRepairPage() {
    const sequence = ++state.repairSequence;
    repairAbortController?.abort();
    const controller = new AbortController();
    repairAbortController = controller;
    setRepairLoading(true);
    setSystemState("loading");
    const params = new URLSearchParams({ page: String(state.page), size: String(state.size) });
    if (state.status) params.set("status", state.status);
    if (state.type) params.set("type", state.type);
    if (state.query) params.set("query", state.query);

    try {
        const page = await requestJson(`/api/repair/page?${params}`, {
            signal: controller.signal,
        });
        if (sequence !== state.repairSequence) return false;
        state.repairs = page.records;
        state.total = page.total;
        state.page = page.page;
        state.loadedRepairPage = page.page;
        state.size = page.size;
        writeRepairStateToLocation("replace");
        state.repairHealthy = true;
        renderRepairs();
        setRepairStale(false);
        syncSystemState();
        return true;
    } catch (error) {
        if (isAbortError(error)) return false;
        if (sequence !== state.repairSequence) return false;
        showToast(error.message || "维修工单加载失败", true);
        state.page = state.loadedRepairPage;
        writeRepairStateToLocation("replace");
        state.repairHealthy = false;
        renderRepairs();
        setRepairStale(true);
        syncSystemState();
        return false;
    } finally {
        if (sequence === state.repairSequence) {
            if (repairAbortController === controller) repairAbortController = undefined;
            setRepairLoading(false);
        }
    }
}

function hasRepairFilters() {
    return Boolean(state.status || state.type || state.query);
}

function updateClearFiltersButton() {
    const status = byId("status-filter").value;
    const type = byId("type-filter").value;
    const query = byId("query-filter").value.trim();
    const parts = [
        status ? `状态：${STATUS_LABELS[status] || status}` : "",
        type ? `类型：${TYPE_LABELS[type] || type}` : "",
        query ? `关键词：“${query}”` : "",
    ].filter(Boolean);
    const active = parts.length > 0;
    elements.filterSummary.hidden = !active;
    elements.filterSummaryText.textContent = parts.join(" · ");
    elements.clearFiltersButton.hidden = !active;
    elements.emptyClearFiltersButton.hidden = !active;
}

function clearRepairFilters() {
    window.clearTimeout(searchTimer);
    byId("status-filter").value = "";
    byId("type-filter").value = "";
    byId("query-filter").value = "";
    state.status = "";
    state.type = "";
    state.query = "";
    state.page = 1;
    updateClearFiltersButton();
    writeRepairStateToLocation("push");
    loadRepairPage();
}

function cancelRepairLoad() {
    state.repairSequence += 1;
    repairAbortController?.abort();
    repairAbortController = undefined;
}

function isAbortError(error) {
    return error?.name === "AbortError";
}

function renderDashboard(summary) {
    updateTodayLabel(new Date(`${summary.date}T12:00:00`));
    byId("metric-pending").textContent = summary.pendingRepairs;
    byId("metric-processing").textContent = summary.processingRepairs;
    byId("metric-check").textContent = summary.waitingCheckRepairs;
    byId("metric-cleaning").textContent = summary.todayCleaningPlans.length;
    byId("repair-total").textContent = summary.totalRepairs;
    byId("urgent-summary").textContent = summary.urgentOpenRepairs > 0
        ? `全局 ${summary.urgentOpenRepairs} 条紧急`
        : "";
    byId("cleaning-date").textContent = formatDate(summary.date);
    byId("last-updated").textContent = `数据更新于 ${formatClock(summary.generatedAt)}`;
    renderOperationBrief(summary);
    renderCleaning(summary.todayCleaningPlans);
}

function renderOperationBrief(summary) {
    const openCount = summary.pendingRepairs + summary.processingRepairs + summary.waitingCheckRepairs;
    const urgentCount = summary.urgentOpenRepairs;
    const brief = byId("operation-brief");
    byId("brief-urgent").textContent = urgentCount;
    byId("brief-open").textContent = openCount;

    if (urgentCount > 0) {
        brief.dataset.tone = "urgent";
        byId("brief-title").textContent = `${urgentCount} 条紧急工单需要优先推进`;
        byId("brief-detail").textContent =
            `先处理紧急报修；当前另有 ${summary.processingRepairs} 条处理中、${summary.waitingCheckRepairs} 条待验收。`;
    } else if (openCount > 0) {
        brief.dataset.tone = "active";
        byId("brief-title").textContent = `今日有 ${openCount} 条维修事项待推进`;
        byId("brief-detail").textContent =
            `${summary.pendingRepairs} 条待派单，${summary.processingRepairs} 条处理中，${summary.waitingCheckRepairs} 条待验收。`;
    } else {
        brief.dataset.tone = "clear";
        byId("brief-title").textContent = "当前没有未结维修工单";
        byId("brief-detail").textContent =
            `今日已安排 ${summary.todayCleaningPlans.length} 项保洁计划，可继续关注新增报修。`;
    }
}

function renderRepairs() {
    const hasRecords = state.repairs.length > 0;
    elements.repairEmpty.hidden = hasRecords;
    const filtered = hasRepairFilters();
    byId("repair-empty-title").textContent = filtered ? "没有匹配的维修工单" : "当前没有维修工单";
    byId("repair-empty-copy").textContent = filtered
        ? "可以清除筛选查看全部工单，或直接新建工单。"
        : "新的报修提交后会出现在这里。";
    elements.emptyClearFiltersButton.hidden = !filtered;
    elements.repairTableBody.innerHTML = hasRecords ? state.repairs.map(repairTableRow).join("") : "";
    elements.repairMobileList.innerHTML = hasRecords ? state.repairs.map(repairMobileRow).join("") : "";
    byId("repair-total").textContent = state.total;

    const first = state.total === 0 ? 0 : (state.page - 1) * state.size + 1;
    const last = Math.min(state.page * state.size, state.total);
    byId("page-summary").textContent = state.total === 0 ? "无结果" : `显示 ${first}–${last}，共 ${state.total} 条`;
    byId("current-page").textContent = state.page;
    byId("previous-page").disabled = state.page <= 1;
    byId("next-page").disabled = state.page * state.size >= state.total;
}

function repairTableRow(order) {
    return `<tr>
        <td><span class="order-title"><strong>${escapeHtml(order.title)}</strong><small>#R${padId(order.id)} · ${escapeHtml(order.reporterName || `用户 ${order.reporterId}`)}</small></span></td>
        <td>${priorityMarkup(order.priority)}</td>
        <td>${escapeHtml(TYPE_LABELS[order.repairType] || order.repairType)}</td>
        <td>${statusMarkup(order.status)}</td>
        <td>${escapeHtml(order.assigneeName || "未指派")}</td>
        <td>${formatDateTime(order.createdAt)}</td>
        <td class="action-column">${repairActionMarkup(order)}</td>
    </tr>`;
}

function repairMobileRow(order) {
    const priority = String(order.priority || "LOW").toLowerCase();
    return `<article class="mobile-repair-item priority-rail-${priority}">
        <div class="mobile-repair-main">
            <div class="mobile-repair-title">${priorityMarkup(order.priority)}<strong>#R${padId(order.id)} · ${escapeHtml(order.title)}</strong></div>
            <div class="mobile-repair-meta">
                <span>${escapeHtml(TYPE_LABELS[order.repairType] || order.repairType)}</span>
                <span>${escapeHtml(order.assigneeName || "未指派")}</span>
                <span>${formatDateTime(order.createdAt)}</span>
            </div>
        </div>
        <div class="mobile-repair-side">${statusMarkup(order.status)}${repairActionMarkup(order)}</div>
    </article>`;
}

function renderCleaning(plans) {
    const pendingCount = plans.filter((plan) => plan.status === "PENDING").length;
    const activeCount = plans.filter((plan) => plan.status === "IN_PROGRESS").length;
    const finishedCount = plans.filter(
        (plan) => plan.status === "COMPLETED" || plan.status === "SKIPPED"
    ).length;
    const progress = [
        `${plans.length} 项`,
        pendingCount > 0 ? `${pendingCount} 待开始` : "",
        activeCount > 0 ? `${activeCount} 进行中` : "",
        finishedCount > 0 ? `${finishedCount} 已结束` : "",
    ].filter(Boolean);
    byId("cleaning-summary").textContent = progress.join(" · ");
    elements.cleaningEmpty.hidden = plans.length > 0;
    elements.cleaningList.innerHTML = plans.map((plan) => {
        const primaryAction = plan.status === "PENDING" ? "start" : plan.status === "IN_PROGRESS" ? "complete" : "";
        const primaryLabel = primaryAction === "start" ? "开始" : "完成";
        const canSkip = plan.status === "PENDING" || plan.status === "IN_PROGRESS";
        return `<li class="cleaning-item">
            <time class="cleaning-time" datetime="${escapeHtml(plan.planDate)}T${escapeHtml(plan.planTime || "00:00")}">${formatPlanTime(plan.planTime)}<small>${formatMonthDay(plan.planDate)}</small></time>
            <div class="cleaning-content"><strong>${escapeHtml(plan.area)}</strong><p>${escapeHtml(plan.cleanerName)}${plan.remark ? ` · ${escapeHtml(plan.remark)}` : ""}</p>${statusMarkup(plan.status)}</div>
            <div class="cleaning-actions"><div>${primaryAction ? `<button type="button" data-cleaning-action="${primaryAction}" data-plan-id="${plan.id}">${primaryLabel}</button>` : ""}${canSkip ? `<button class="secondary" type="button" data-cleaning-action="skip" data-plan-id="${plan.id}">跳过</button>` : ""}</div></div>
        </li>`;
    }).join("");
}

function repairActionMarkup(order) {
    const actions = {
        PENDING: ["assign", "派单"],
        PROCESSING: ["complete", "完工"],
        WAITING_CHECK: ["verify", "验收"],
    };
    const action = actions[order.status];
    if (!action) return '<span class="status status-completed">已归档</span>';
    return `<button class="row-action" type="button" data-repair-action="${action[0]}" data-order-id="${order.id}">${action[1]}</button>`;
}

function priorityMarkup(priority) {
    const value = String(priority || "LOW").toLowerCase();
    return `<span class="priority priority-${value}">${escapeHtml(PRIORITY_LABELS[priority] || priority)}</span>`;
}

function statusMarkup(status) {
    return `<span class="status status-${String(status).toLowerCase()}">${escapeHtml(STATUS_LABELS[status] || status)}</span>`;
}

function openNewRepairDialog() {
    openDialog({
        title: "新建维修工单",
        subtitle: "提交后工单将进入待处理队列。",
        submitLabel: "创建工单",
        fields: `<label>工单标题 <span>*</span><input name="title" required maxlength="100" autocomplete="off" placeholder="例如：卫生间水龙头漏水"></label>
            <label>问题描述<textarea name="description" maxlength="500" placeholder="补充位置、现象和影响范围"></textarea></label>
            <div class="field-row">
                <label>维修类型 <span>*</span><select name="repairType" required><option value="PLUMBING">水电维修</option><option value="FURNITURE">家具维修</option><option value="APPLIANCE">电器维修</option><option value="NETWORK">网络维修</option><option value="OTHER">其他</option></select></label>
                <label>优先级 <span>*</span><select name="priority" required><option value="NORMAL">普通</option><option value="URGENT">紧急</option><option value="LOW">低</option></select></label>
            </div>
            <label>报修人 ID <span>*</span><input name="reporterId" type="number" min="1" value="1" required inputmode="numeric"></label>`,
        onSubmit: async (form) => requestJson("/api/repair/report", {
            method: "POST",
            body: JSON.stringify({
                title: form.get("title"),
                description: form.get("description"),
                repairType: form.get("repairType"),
                priority: form.get("priority"),
                reporterId: Number(form.get("reporterId")),
            }),
        }),
    });
}

function openNewCleaningDialog() {
    const today = state.dashboard?.date || new Date().toISOString().slice(0, 10);
    openDialog({
        title: "新增保洁计划",
        subtitle: "计划按日期保存；今天的计划会按执行时间加入今日列表。",
        submitLabel: "创建计划",
        fields: `<label>保洁区域 <span>*</span><input name="area" required maxlength="100" placeholder="例如：一号公寓三楼走廊"></label>
            <div class="field-row"><label>保洁人员 <span>*</span><input name="cleanerName" required maxlength="64" placeholder="姓名"></label><label>计划日期 <span>*</span><input name="planDate" type="date" required value="${escapeHtml(today)}"></label></div>
            <label>计划时间<input name="planTime" type="time"></label>
            <label>备注<textarea name="remark" maxlength="500" placeholder="清洁范围或注意事项"></textarea></label>`,
        onSubmit: async (form) => {
            const payload = {
                area: form.get("area"),
                cleanerName: form.get("cleanerName"),
                planDate: form.get("planDate"),
                remark: form.get("remark"),
            };
            if (form.get("planTime")) payload.planTime = form.get("planTime");
            return requestJson("/api/cleaning/plans", { method: "POST", body: JSON.stringify(payload) });
        },
    });
}

function openRepairActionDialog(order, action) {
    const reference = `#R${padId(order.id)} · ${order.title}`;
    if (action === "assign") {
        openDialog({
            title: "派单",
            subtitle: reference,
            submitLabel: "确认派单",
            fields: `<label>指派给（用户 ID） <span>*</span><input name="assigneeId" type="number" min="1" value="2" required inputmode="numeric"></label><p class="dialog-help">仅可将待处理工单指派给系统中的有效用户。</p>`,
            onSubmit: (form) => requestJson("/api/repair/assign", { method: "PUT", body: JSON.stringify({ orderId: order.id, assigneeId: Number(form.get("assigneeId")) }) }),
        });
        return;
    }
    if (action === "complete") {
        openDialog({
            title: "登记维修完成",
            subtitle: reference,
            submitLabel: "提交完工",
            fields: `<div class="field-row"><label>维修费 <span>*</span><input name="repairFee" type="number" min="0" max="99999999.99" step="0.01" value="0" required></label><label>材料费 <span>*</span><input name="materialFee" type="number" min="0" max="99999999.99" step="0.01" value="0" required></label></div><p class="dialog-help">提交后工单进入待验收状态，系统会自动计算总费用。</p>`,
            onSubmit: (form) => requestJson("/api/repair/complete", { method: "PUT", body: JSON.stringify({ orderId: order.id, repairFee: Number(form.get("repairFee")), materialFee: Number(form.get("materialFee")) }) }),
        });
        return;
    }
    openDialog({
        title: "验收维修工单",
        subtitle: reference,
        submitLabel: "确认验收",
        fields: '<p>确认维修结果符合要求？验收后工单将归档为已完成。</p>',
        onSubmit: () => requestJson("/api/repair/verify", { method: "PUT", body: JSON.stringify({ orderId: order.id }) }),
    });
}

function openCleaningActionDialog(plan, action) {
    const labels = { start: ["开始保洁", "确认开始"], complete: ["完成保洁", "确认完成"], skip: ["跳过计划", "确认跳过"] };
    const [title, submitLabel] = labels[action];
    openDialog({
        title,
        subtitle: `${plan.area} · ${plan.cleanerName}`,
        submitLabel,
        fields: `<p>将“${escapeHtml(plan.area)}”更新为“${submitLabel.replace("确认", "")}”状态？</p>`,
        onSubmit: () => requestJson(`/api/cleaning/plans/${plan.id}/${action}`, { method: "PUT" }),
    });
}

function openDialog({ title, subtitle, submitLabel, fields, onSubmit }) {
    dialogReturnFocus = document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null;
    byId("dialog-title").textContent = title;
    byId("dialog-subtitle").textContent = subtitle;
    elements.dialogFields.innerHTML = fields;
    elements.dialogSubmit.textContent = submitLabel;
    elements.dialogSubmit.dataset.defaultLabel = submitLabel;
    state.dialogSubmit = onSubmit;
    elements.dialog.showModal();
    window.setTimeout(() => elements.dialogFields.querySelector("input, select, textarea")?.focus(), 20);
}

function closeDialog() {
    if (elements.dialog.open) elements.dialog.close();
    state.dialogSubmit = null;
    const focusTarget = dialogReturnFocus;
    dialogReturnFocus = null;
    if (focusTarget?.isConnected) {
        window.setTimeout(() => focusTarget.focus(), 0);
    }
}

function setDialogLoading(loading) {
    elements.dialogForm.querySelectorAll("input, select, textarea, button").forEach((control) => {
        control.disabled = loading;
    });
    elements.dialogSubmit.textContent = loading ? "处理中…" : elements.dialogSubmit.dataset.defaultLabel;
    elements.dialogSubmit.setAttribute("aria-busy", String(loading));
    elements.dialogForm.setAttribute("aria-busy", String(loading));
}

function setRepairLoading(loading) {
    elements.repairPanel.setAttribute("aria-busy", String(loading));
    if (!loading || state.repairs.length > 0) return;
    elements.repairEmpty.hidden = true;
    elements.repairMobileList.innerHTML = "";
    elements.repairTableBody.innerHTML = Array.from({ length: 5 }, () => '<tr class="loading-row"><td colspan="7"><span></span></td></tr>').join("");
}

function setRepairStale(stale) {
    elements.repairSyncNote.textContent = state.repairs.length > 0
        ? "数据同步失败，正在显示上次成功结果。"
        : "数据同步失败，请稍后重试。";
    elements.repairSyncNote.hidden = !stale;
}

function renderLoadFailure() {
    state.repairs = [];
    state.total = 0;
    renderRepairs();
    elements.cleaningList.innerHTML = "";
    elements.cleaningEmpty.hidden = false;
    byId("cleaning-summary").textContent = "同步失败";
    byId("last-updated").textContent = "数据同步失败";
    byId("operation-brief").dataset.tone = "error";
    byId("brief-title").textContent = "暂时无法生成今日重点";
    byId("brief-detail").textContent = "请稍后刷新，系统会保留最近一次成功加载的数据。";
    byId("brief-urgent").textContent = "—";
    byId("brief-open").textContent = "—";
}

async function requestJson(path, options = {}) {
    const response = await fetch(path, {
        ...options,
        headers: { Accept: "application/json", ...(options.body ? { "Content-Type": "application/json" } : {}), ...options.headers },
    });
    const contentType = response.headers.get("content-type") || "";
    const payload = contentType.includes("application/json") ? await response.json() : null;
    if (!response.ok) {
        const fieldMessage = payload?.errors ? Object.values(payload.errors).filter(Boolean).join("；") : "";
        throw new Error(fieldMessage || payload?.message || `请求失败（${response.status}）`);
    }
    return payload;
}

function setActiveTab(tab) {
    if (!TAB_IDS.has(tab)) return;
    state.activeTab = tab;
    document.querySelectorAll("[data-tab]").forEach((button) => {
        const active = button.dataset.tab === tab;
        button.classList.toggle("is-active", active);
        button.setAttribute("aria-selected", String(active));
        button.tabIndex = active ? 0 : -1;
    });
    byId("repairs").classList.toggle("is-mobile-hidden", tab !== "repairs");
    byId("cleaning").classList.toggle("is-mobile-hidden", tab !== "cleaning");
    setNavigationDestination(tab);
}

function navigateToTab(tab) {
    if (!TAB_IDS.has(tab)) return;
    const hash = `#${tab}`;
    if (window.location.hash !== hash) {
        window.history.pushState(null, "", hash);
    }
    setActiveTab(tab);
}

function syncNavigationFromLocation() {
    const destination = window.location.hash.slice(1);
    if (TAB_IDS.has(destination)) {
        setActiveTab(destination);
        return;
    }
    setActiveTab("repairs");
    setNavigationDestination("overview");
}

function setNavigationDestination(destination) {
    document.querySelectorAll("[data-nav]").forEach((link) => {
        link.classList.toggle("is-active", link.dataset.nav === destination);
    });
}

function setSidebarOpen(open, restoreFocus = false) {
    elements.sidebar.classList.toggle("is-open", open);
    elements.sidebarScrim.hidden = !open;
    elements.menuButton.setAttribute("aria-expanded", String(open));
    elements.menuButton.setAttribute("aria-label", open ? "关闭导航" : "打开导航");
    if (!open && restoreFocus) elements.menuButton.focus();
}

function showToast(message, isError = false) {
    window.clearTimeout(toastTimer);
    byId("toast-message").textContent = message;
    elements.toast.classList.toggle("is-error", isError);
    elements.toast.setAttribute("role", isError ? "alert" : "status");
    elements.toast.setAttribute("aria-live", isError ? "assertive" : "polite");
    elements.toast.querySelector(".toast-icon").textContent = isError ? "!" : "✓";
    elements.toast.hidden = false;
    toastTimer = window.setTimeout(hideToast, 4200);
}

function hideToast() {
    elements.toast.hidden = true;
}

function setSystemState(status) {
    const labels = {
        loading: "系统状态：正在同步",
        normal: "系统状态：正常",
        partial: "系统状态：部分数据异常",
        error: "系统状态：连接异常",
    };
    elements.systemState.dataset.state = status;
    elements.systemStateLabel.textContent = labels[status] || labels.error;
}

function syncSystemState() {
    if (state.dashboardHealthy === false && state.repairHealthy !== true) {
        setSystemState("error");
    } else if (state.dashboardHealthy === false || state.repairHealthy === false) {
        setSystemState("partial");
    } else if (state.dashboardHealthy === true) {
        setSystemState("normal");
    } else {
        setSystemState("loading");
    }
}

function updateTodayLabel(date) {
    byId("today-label").textContent = new Intl.DateTimeFormat("zh-CN", { year: "numeric", month: "long", day: "numeric", weekday: "long" }).format(date);
}

function formatDate(value) {
    return new Intl.DateTimeFormat("zh-CN", { month: "long", day: "numeric", weekday: "long" }).format(new Date(`${value}T12:00:00`));
}

function formatMonthDay(value) {
    return new Intl.DateTimeFormat("zh-CN", { month: "2-digit", day: "2-digit" }).format(new Date(`${value}T12:00:00`));
}

function formatDateTime(value) {
    if (!value) return "—";
    return new Intl.DateTimeFormat("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", hour12: false }).format(new Date(value));
}

function formatClock(value) {
    if (!value) return "—";
    return new Intl.DateTimeFormat("zh-CN", {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
        hour12: false,
    }).format(new Date(value));
}

function formatPlanTime(value) {
    return value ? String(value).slice(0, 5) : "待定";
}

function padId(value) {
    return String(value).padStart(6, "0");
}

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>'"]/g, (character) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" })[character]);
}

init();

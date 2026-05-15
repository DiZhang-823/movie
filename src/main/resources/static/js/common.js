// ⚠️ 这里改成你的真实后端路径
const API_BASE = "/movies";

async function loadSidebar(active) {
    const res = await fetch("components/sidebar.html");
    const html = await res.text();

    document.getElementById("sidebar-container").innerHTML = html;

    // 设置高亮
    if (active) {
        const el = document.getElementById(`menu-${active}`);
        if (el) el.classList.add("active");
    }
}
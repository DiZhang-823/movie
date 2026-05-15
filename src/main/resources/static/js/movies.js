window.onload = loadMovies;

async function loadMovies() {
    const res = await fetch(`${API_BASE}?page=0&size=10`);
    const data = await res.json();
    renderMovies(data.content);
}

function renderMovies(list) {
    const body = document.getElementById("movieTableBody");
    body.innerHTML = "";

    list.forEach(m => {
        body.innerHTML += `
            <tr>
                <td>${m.id ?? ""}</td>
                <td>${m.title ?? ""}</td>
                <td>${m.genre ?? ""}</td>
                <td>${m.year ?? ""}</td>
                <td>${m.rating ?? ""}</td>
                <td>
                    <div class="action-buttons">
                        <button class="btn btn-edit" onclick="editMovie(${m.id})">Edit</button>
                        <button class="btn btn-delete" onclick="deleteMovie(${m.id})">Delete</button>
                    </div>
                </td>
            </tr>
        `;
    });
}

function openAddModal() {
    resetForm();
    document.getElementById("modalTitle").innerText = "Add Movie";
    document.getElementById("movieModal").classList.remove("hidden");
}

function closeMovieModal() {
    document.getElementById("movieModal").classList.add("hidden");
}

async function saveMovie() {
    const id = document.getElementById("movieId").value;

    const data = {
        title: document.getElementById("title").value,
        genre: document.getElementById("genre").value,
        year: document.getElementById("year").value ? Number(document.getElementById("year").value) : null,
        rating: document.getElementById("rating").value ? Number(document.getElementById("rating").value) : null
    };

    const url = id ? `${API_BASE}/${id}` : API_BASE;
    const method = id ? "PUT" : "POST";

    const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
    });

    if (!res.ok) {
        const errorText = await res.text();
        alert("Save failed: " + errorText);
        return;
    }

    closeMovieModal();
    resetForm();
    loadMovies();
}

async function editMovie(id) {
    const res = await fetch(`${API_BASE}/${id}`);
    const m = await res.json();

    document.getElementById("movieId").value = m.id ?? "";
    document.getElementById("title").value = m.title ?? "";
    document.getElementById("genre").value = m.genre ?? "";
    document.getElementById("year").value = m.year ?? "";
    document.getElementById("rating").value = m.rating ?? "";

    document.getElementById("modalTitle").innerText = "Edit Movie";
    document.getElementById("movieModal").classList.remove("hidden");
}

async function deleteMovie(id) {
    const ok = confirm("Are you sure you want to delete this movie?");
    if (!ok) return;

    const res = await fetch(`${API_BASE}/${id}`, {
        method: "DELETE"
    });

    if (!res.ok) {
        alert("Delete failed");
        return;
    }

    loadMovies();
}

async function searchMovies() {
    const keyword = document.getElementById("searchTitle").value.trim();
    const res = await fetch(`${API_BASE}?title=${encodeURIComponent(keyword)}&page=0&size=10`);
    const data = await res.json();
    renderMovies(data.content);
}

function resetForm() {
    document.getElementById("movieId").value = "";
    document.getElementById("title").value = "";
    document.getElementById("genre").value = "";
    document.getElementById("year").value = "";
    document.getElementById("rating").value = "";
}
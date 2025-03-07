const apiBaseUrl = "http://localhost:8080/api/chat";

function handleKeyPress(event) {
    if (event.key === "Enter") {
        sendMessage();
    }
}

function createNewChat() {
    const firstQuestion = prompt("Enter your first question:");
    if (firstQuestion) {
        fetch(`${apiBaseUrl}/new?firstQuestion=${encodeURIComponent(firstQuestion)}`, { method: "POST" })
            .then(response => response.json())
            .then(chatId => {
                alert("Chat session created with ID: " + chatId);
                fetchAllChatSessions();
                selectChatSession(chatId);
            });
    }
}

function sendMessage() {
    const chatId = document.getElementById("chat-sessions").value;
    const question = document.getElementById("user-input").value;

    if (!chatId || !question.trim()) {
        alert("Please select a chat session and enter a message.");
        return;
    }

    fetch(`${apiBaseUrl}/${chatId}/ask?question=${encodeURIComponent(question)}`)
        .then(response => response.json())
        .then(data => {
            displayChatMessage(question, formatChatbotResponse(data.answer));
            document.getElementById("user-input").value = "";
        });
}

function displayChatMessage(question, answer) {
    const chatWrapper = document.getElementById("chat-wrapper");

    const messageContainer = document.createElement("div");
    messageContainer.classList.add("chat-message");

    const questionElement = document.createElement("div");
    questionElement.classList.add("question");
    questionElement.textContent = "Q: " + question;

    const answerElement = document.createElement("div");
    answerElement.classList.add("answer");
    answerElement.innerHTML = "A: " + answer;

    messageContainer.appendChild(questionElement);
    messageContainer.appendChild(answerElement);
    chatWrapper.appendChild(messageContainer);

    chatWrapper.scrollTop = chatWrapper.scrollHeight;
}

function formatChatbotResponse(response) {
    response = response.replace(/```([\s\S]*?)```/g, "<pre><code>$1</code></pre>");
    response = response.replace(/\n\s*\*\s\*\*(.*?)\*\*(.*?)/g, "<li><strong>$1:</strong>$2</li>");
    response = response.replace(/\n\s*\*\s(.*?)/g, "<li>$1</li>");
    response = response.replace(/(<li>.*?<\/li>)+/g, "<ul>$&</ul>");
    response = response.replace(/\*(.*?)\*/g, "<em>$1</em>");
    return response;
}
function toggleTheme() {
    const body = document.body;
    const toggleButton = document.querySelector(".theme-toggle");

    body.classList.toggle("dark-mode");

    if (body.classList.contains("dark-mode")) {
        localStorage.setItem("theme", "dark");
        toggleButton.textContent = "☀️ Light Mode";
    } else {
        localStorage.setItem("theme", "light");
        toggleButton.textContent = "🌙 Dark Mode";
    }
}

document.addEventListener("DOMContentLoaded", () => {
    const savedTheme = localStorage.getItem("theme");
    const toggleButton = document.querySelector(".theme-toggle");

    if (savedTheme === "dark") {
        document.body.classList.add("dark-mode");
        toggleButton.textContent = "☀️ Light Mode";
    }
});






// Load Previous Chats (Pagination)
function fetchPaginatedQuestions() {
    const chatId = document.getElementById("chat-sessions").value;
    if (!chatId) {
        alert("Please select a chat session.");
        return;
    }

    fetch(`${apiBaseUrl}/${chatId}/questions?page=0&size=10`)
        .then(response => response.json())
        .then(data => {
            document.getElementById("chat-wrapper").innerHTML = "";
            data.content.forEach(item => {
                displayChatMessage(item.question, formatChatbotResponse(item.answer));
            });
        });
}

// Fetch All Chat Sessions
function fetchAllChatSessions() {
    fetch(`${apiBaseUrl}/allChatIds`)
        .then(response => response.json())
        .then(data => {
            const sessionDropdown = document.getElementById("chat-sessions");
            sessionDropdown.innerHTML = `<option value="">Select Chat Session</option>`;
            data.forEach(chatId => {
                sessionDropdown.innerHTML += `<option value="${chatId}">Chat ${chatId}</option>`;
            });
        });
}


// Select a Chat Session
function selectChatSession(chatId) {
    if (chatId) {
        document.getElementById("chat-sessions").value = chatId;
    }
    fetchPaginatedQuestions();
}

// Delete a Chat
function deleteChat() {
    const chatId = document.getElementById("chat-sessions").value;
    if (!chatId) {
        alert("Please select a chat session to delete.");
        return;
    }

    fetch(`${apiBaseUrl}/${chatId}`, { method: "DELETE" })
        .then(() => {
            alert("Chat deleted!");
            fetchAllChatSessions();
            document.getElementById("chat-wrapper").innerHTML = "";
        });
}

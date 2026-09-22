<#include "header.ftl">

<div class="row justify-content-center align-items-center min-vh-100">
  <div class="bg-light col-5 p-3 rounded shadow-sm">
    <div class="container">

      <h2 class="mb-4">${(i18n["page.login.title"])!"Log In"}</h2>

      <form action="/login" method="POST">
        <div id="error-message" class="alert alert-danger" style="display: none;">
          ${(i18n["page.login.error.invalid_credentials"]!"Invalid username or password.")}
        </div>

        <div class="mb-3">
          <label for="username" class="form-label">${(i18n["page.login.username"])!"Username"}</label>
          <input type="text" id="username" name="username" class="form-control" placeholder="${(i18n["page.login.username.placeholder"])!"Enter your username"}" required>
        </div>

        <div class="mb-3">
          <label for="password" class="form-label">${(i18n["page.login.password"])!"Password"}</label>
          <input type="password" id="password" name="password" class="form-control" placeholder="${(i18n["page.login.password.placeholder"])!"Enter your password"}" required>
        </div>

        <button type="submit" class="btn btn-primary mb-3 w-100">${(i18n["page.login.button"])!"Log in"}</button>
      </form>

      <hr>
      <p>${(i18n["page.login.new_here"])!"New here?"}</p>
      <a href="/register" class="btn btn-secondary">${(i18n["page.login.register"])!"Register"}</a>

    </div>
  </div>
</div>

<script>
  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get('error') === 'true') {
    document.getElementById('error-message').style.display = 'block';
  }
</script>

<#include "footer.ftl">

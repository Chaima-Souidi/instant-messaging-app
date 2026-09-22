<#include "header.ftl">

<div class="row justify-content-center align-items-center min-vh-100">
  <div class="bg-light col-5 p-3 rounded shadow-sm">
    <div class="container">

      <h2 class="mb-4">${(i18n["page.register.title"])!"Register"}</h2>

      <form action="/register" method="POST">
        <div class="mb-3">
          <label for="username" class="form-label">${(i18n["page.register.username"])!"Username"}</label>
          <input type="text" id="username" name="username" class="form-control" placeholder="${(i18n["page.register.username.placeholder"])!"Choose a username"}" required>
        </div>

        <div class="mb-3">
          <label for="password" class="form-label">${(i18n["page.register.password"])!"Password"}</label>
          <input type="password" id="password" name="password" class="form-control" placeholder="${(i18n["page.register.password.placeholder"])!"Create a password"}" required>
        </div>

        <button type="submit" class="btn btn-primary mb-3 w-100">${(i18n["page.register.button"])!"Register"}</button>
      </form>

      <hr>
      <p>${(i18n["page.register.already_account"])!"Already have an account?"}</p>
      <a href="/login" class="btn btn-secondary">${(i18n["page.register.login"])!"Log In"}</a>

    </div>
  </div>
</div>

<#include "footer.ftl">

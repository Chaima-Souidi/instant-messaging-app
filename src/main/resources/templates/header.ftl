<!DOCTYPE html>
<html lang="en"> <head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
  <meta http-equiv="x-ua-compatible" content="ie=edge">
  <link href="https://cdnjs.cloudflare.com/ajax/libs/bootswatch/5.3.8/minty/bootstrap.min.css" rel="stylesheet" integrity="sha512-2rlhob8L606Mt1c8oB20JR78cIX27wzWelusr5yq+4VX6ly4F3Tr88Vhj8B63yxB7k8i1BRq3fKaiaYESpo8rA" crossorigin="anonymous">
  <title>${title} | Instant Messaging</title>
</head>
<body>

<div class="bg-primary text-white p-4 sticky-top">
  <div class="container d-flex align-items-center justify-content-between">

    <div class="d-flex gap-2">
      <a href="/" class="btn btn-outline-light">${(i18n["header.nav.home"])!"Home"}</a>
      <a href="/login" class="btn btn-outline-light">${(i18n["header.nav.login"])!"Log In"}</a>
      <a href="/register" class="btn btn-outline-light">${(i18n["header.nav.register"])!"Register"}</a>
    </div>

    <h2 class="mb-0">Instant Messaging</h2>

    <div class="dropdown">
      <button class="btn btn-outline-light dropdown-toggle" type="button" id="languageDropdown" data-bs-toggle="dropdown" aria-expanded="false">
        ${(i18n["header.nav.language"])!"Language"}
      </button>
      <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="languageDropdown">
        <li><a class="dropdown-item" href="/lang/fr">Français</a></li>
        <li><a class="dropdown-item" href="/lang/en">English</a></li>
        <li><a class="dropdown-item" href="/lang/es">Español</a></li>
      </ul>
    </div>

  </div>
</div>

<div class="container p-4">

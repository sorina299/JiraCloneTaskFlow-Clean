// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { AuthGuard } from './guards/auth,guard';
import { NoAuthGuard } from './guards/no-auth.guard';
import { LoginComponent } from './pages/login/login/login.component';
import { RegisterComponent } from './pages/register/register/register.component';
import { PageNotFoundComponent } from './pages/page-not-found/page-not-found/page-not-found.component';
import { ProjectsComponent } from './pages/projects/projects/projects.component';

export const routes: Routes = [
  { path: '', redirectTo: 'projects', pathMatch: 'full' },

  {
    path: '',
    canActivate: [AuthGuard],
    children: [
      { path: 'projects', component: ProjectsComponent },
    ],
  },

  {
    path: '',
    canActivate: [NoAuthGuard],
    children: [
      { path: 'login', component: LoginComponent },
      { path: 'register', component: RegisterComponent },
    ],
  },

  { path: '**', component: PageNotFoundComponent },
];

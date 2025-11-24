// src/app/services/authentication-service/authentication.service.ts
import { Injectable } from '@angular/core';
import { ApiService } from '../api-service/api.service';
import { BehaviorSubject, Observable, catchError, tap, throwError } from 'rxjs';
import { Router } from '@angular/router';

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
}

@Injectable({ providedIn: 'root' })
export class AuthenticationService {
  private accessToken: string | null = null;

  private authStatusSubject = new BehaviorSubject<boolean>(
    typeof window !== 'undefined' && !!localStorage.getItem('access_token')
  );
  authStatus$ = this.authStatusSubject.asObservable();

  constructor(private api: ApiService, private router: Router) {}

  setToken(accessToken: string, refreshToken: string): void {
    this.accessToken = accessToken;

    if (typeof window !== 'undefined') {
      localStorage.setItem('access_token', accessToken);
      localStorage.setItem('refresh_token', refreshToken);
    }

    this.authStatusSubject.next(true);
  }

  getToken(): string | null {
    if (typeof window === 'undefined') return null;
    return this.accessToken ?? localStorage.getItem('access_token');
  }

  clearTokens(): void {
    this.accessToken = null;
    if (typeof window !== 'undefined') {
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
    }
    this.authStatusSubject.next(false);
  }

  isAuthenticated(): boolean {
    return typeof window !== 'undefined' && !!localStorage.getItem('access_token');
  }

  logout(): void {
    this.clearTokens();
    this.router.navigate(['/login']);
  }

  register(data: {
    username: string;
    email: string;
    password: string;
    firstName: string;
    lastName: string;
  }): Observable<any> {
    return this.api.post('/auth/register', data);
  }

  login(credentials: { username: string; password: string }): Observable<AuthResponse> {
    return this.api.post<AuthResponse>('/auth/login', credentials).pipe(
      tap((res) => this.setToken(res.accessToken, res.refreshToken))
    );
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken =
      typeof window !== 'undefined' ? localStorage.getItem('refresh_token') : null;

    if (!refreshToken) {
      this.logout();
      return throwError(() => new Error('No refresh token available'));
    }

    return this.api
      .post<AuthResponse>('/auth/refresh', { refreshToken })
      .pipe(
        tap((res) => this.setToken(res.accessToken, res.refreshToken)),
        catchError((err) => {
          this.logout();
          return throwError(() => err);
        })
      );
  }

  private decodeToken(): any {
    const token = this.getToken();
    if (!token) return null;
    try {
      const payload = token.split('.')[1];
      return JSON.parse(atob(payload));
    } catch {
      return null;
    }
  }

  getUsername(): string {
    const decoded = this.decodeToken();
    return decoded?.sub ?? 'Guest';
  }

  getUserRole(): string {
    const decoded = this.decodeToken();
    return decoded?.role ?? 'UNKNOWN';
  }
}
// src/app/services/authentication-service/token.interceptor.ts
import {
  HttpInterceptorFn,
  HttpRequest,
  HttpHandlerFn,
  HttpEvent,
  HttpErrorResponse,
} from '@angular/common/http';
import { inject } from '@angular/core';
import {
  Observable,
  BehaviorSubject,
  catchError,
  switchMap,
  throwError,
} from 'rxjs';
import { AuthenticationService } from './authentication.service';

const refreshInProgress$ = new BehaviorSubject<boolean>(false);

export const tokenInterceptor: HttpInterceptorFn = (
  req: HttpRequest<any>,
  next: HttpHandlerFn
): Observable<HttpEvent<any>> => {
  const auth = inject(AuthenticationService);

  if (
    req.url.includes('/auth/login') ||
    req.url.includes('/auth/register') ||
    req.url.includes('/auth/refresh')
  ) {
    return next(req);
  }

  const token = auth.getToken();

  let authReq = req;
  if (token) {
    authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }

  return next(authReq).pipe(
    catchError((error) => handleAuthError(error, authReq, next, auth))
  );
};

function handleAuthError(
  error: any,
  req: HttpRequest<any>,
  next: HttpHandlerFn,
  auth: AuthenticationService
): Observable<HttpEvent<any>> {
  if (error instanceof HttpErrorResponse && error.status === 401) {
    if (req.url.includes('/auth/login')) {
      return throwError(() => error);
    }

    if (!refreshInProgress$.value) {
      refreshInProgress$.next(true);

      return auth.refreshToken().pipe(
        switchMap((res) => {
          refreshInProgress$.next(false);
          const newReq = req.clone({
            setHeaders: { Authorization: `Bearer ${res.accessToken}` },
          });
          return next(newReq);
        }),
        catchError((err) => {
          refreshInProgress$.next(false);
          auth.logout();
          return throwError(() => err);
        })
      );
    }
  }

  return throwError(() => error);
}
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useTimeMachine } from '../context/TimeMachineContext';
import { useAuth } from '../contexts/AuthContext';
import styles from './Layout.module.css';

const NAV_MAIN = [
  { to: '/', label: 'Portfolio', end: true },
  { to: '/contracts', label: 'Contracts' },
];

const NAV_OPERATIONS = [
  { to: '/import', label: 'Import' },
  { to: '/reconcile', label: 'Reconciliation' },
];

const NAV_REPORTING = [
  { to: '/reports', label: 'Reports' },
  { to: '/journal', label: 'Journal' },
];

const NAV_ADMIN_ALL = [
  { to: '/settings', label: 'Settings' },
];

const NAV_ADMIN_ONLY = [
  { to: '/admin/users', label: 'Users' },
  { to: '/admin/reference-data', label: 'Reference Data' },
  { to: '/admin/fiscal-years', label: 'Fiscal Years' },
  { to: '/admin/audit', label: 'Audit Log' },
];

function NavItem({ to, label, end }: { to: string; label: string; end?: boolean }) {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) =>
        isActive ? `${styles.navItem} ${styles.navItemActive}` : styles.navItem
      }
    >
      {label}
    </NavLink>
  );
}

function NavSection({ label, items }: { label: string; items: { to: string; label: string }[] }) {
  return (
    <>
      <div className={styles.navSection}>{label}</div>
      {items.map(item => <NavItem key={item.to} to={item.to} label={item.label} />)}
    </>
  );
}

export default function Layout() {
  const { asOfDate, setAsOfDate } = useTimeMachine();
  const { role, displayName, logout } = useAuth();
  const navigate = useNavigate();

  const initials = displayName
    ? displayName.split(' ').map((n: string) => n[0]).join('').slice(0, 2).toUpperCase()
    : '?';

  return (
    <div className={styles.shell}>
      <div className={styles.body}>

        {/* ── Side navigation ────────────────────────────────────────── */}
        <nav className={styles.sideNav}>
          <div className={styles.navHeader} onClick={() => navigate('/')}>
            LEDGER
          </div>

          <div className={styles.navLinks}>
            {NAV_MAIN.map(item => (
              <NavItem key={item.to} to={item.to} label={item.label} end={item.end} />
            ))}

            <NavSection label="OPERATIONS" items={NAV_OPERATIONS} />
            <NavSection label="REPORTING"  items={NAV_REPORTING} />
            <NavSection label="ADMIN"      items={NAV_ADMIN_ALL} />
            {role === 'ADMIN' && NAV_ADMIN_ONLY.map(item => (
              <NavItem key={item.to} to={item.to} label={item.label} />
            ))}
          </div>

          {/* ── User footer ── */}
          <div className={styles.navFooter}>
            <div className={styles.avatar}>{initials}</div>
            <div className={styles.userDetails}>
              <span className={styles.userName}>{displayName ?? 'User'}</span>
              {role && <span className={styles.userRole}>{role}</span>}
            </div>
            <button className={styles.signOutBtn} onClick={logout} title="Sign out" aria-label="Sign out">
              ⇥
            </button>
          </div>
        </nav>

        {/* ── Content column ─────────────────────────────────────────── */}
        <div className={styles.contentColumn}>

          {/* Time machine banner — shown above top bar when active */}
          {asOfDate && (
            <div className={styles.timeMachineBanner}>
              <span>
                ⏱ TIME MACHINE — Viewing as of: <strong>{asOfDate}</strong>
              </span>
              <button className={styles.bannerReset} onClick={() => setAsOfDate(null)}>
                Reset
              </button>
            </div>
          )}

          {/* Top bar */}
          <header className={styles.topBar}>
            <div className={styles.topBarLeft} />
            <div className={styles.topBarRight}>
              <label className={styles.timeMachineControl}>
                <span className={styles.timeMachineLabel}>Time Machine</span>
                <input
                  type="date"
                  className={styles.timeMachineInput}
                  value={asOfDate ?? ''}
                  max={new Date().toISOString().split('T')[0]}
                  onChange={e => setAsOfDate(e.target.value || null)}
                />
              </label>
            </div>
          </header>

          <main className={styles.content}>
            <Outlet />
          </main>
        </div>

      </div>
    </div>
  );
}

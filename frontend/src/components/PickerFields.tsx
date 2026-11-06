import { Children, isValidElement, useEffect, useId, useLayoutEffect, useRef, useState, type CSSProperties, type Dispatch, type KeyboardEvent, type ReactNode, type RefObject, type SetStateAction } from 'react';
import { createPortal } from 'react-dom';
import { CalendarDays, Check, ChevronDown, ChevronLeft, ChevronRight } from 'lucide-react';
import './PickerFields.css';

function usePickerPopover(
  anchor: RefObject<HTMLElement | null>, panel: RefObject<HTMLDivElement | null>,
  focus: RefObject<HTMLElement | null>, open: boolean, setOpen: Dispatch<SetStateAction<boolean>>, width?: number,
) {
  const [position, setPosition] = useState<CSSProperties>({});
  useLayoutEffect(() => {
    if (!open) return;
    const place = () => {
      const field = anchor.current, popup = panel.current;
      if (!field || !popup) return;
      const rect = field.getBoundingClientRect();
      const modal = field.closest('[aria-modal="true"]')?.getBoundingClientRect();
      if (rect.bottom < (modal?.top ?? 0) || rect.top > (modal?.bottom ?? window.innerHeight)) {
        setOpen(false); return;
      }
      const popupWidth = Math.min(width ?? Math.max(rect.width, 220), window.innerWidth - 24);
      popup.style.width = `${popupWidth}px`;
      const below = window.innerHeight - rect.bottom - 16, above = rect.top - 16;
      const upwards = below < Math.min(popup.scrollHeight, 280) && above > below;
      const maxHeight = Math.max(100, upwards ? above : below);
      const height = Math.min(popup.scrollHeight, maxHeight);
      setPosition({ width: popupWidth, maxHeight, left: Math.max(12, Math.min(rect.left, window.innerWidth - popupWidth - 12)), top: upwards ? rect.top - height - 8 : rect.bottom + 8 });
    };
    const outside = (event: PointerEvent) => {
      if (!anchor.current?.contains(event.target as Node) && !panel.current?.contains(event.target as Node)) setOpen(false);
    };
    const key = (event: globalThis.KeyboardEvent) => {
      if (event.key !== 'Escape') return;
      event.preventDefault(); event.stopPropagation(); setOpen(false); focus.current?.focus();
    };
    const scroll = (event: Event) => { if (!panel.current?.contains(event.target as Node)) place(); };
    place();
    document.addEventListener('pointerdown', outside);
    document.addEventListener('keydown', key, true);
    window.addEventListener('resize', place);
    window.addEventListener('scroll', scroll, true);
    return () => {
      document.removeEventListener('pointerdown', outside);
      document.removeEventListener('keydown', key, true);
      window.removeEventListener('resize', place);
      window.removeEventListener('scroll', scroll, true);
    };
  }, [anchor, panel, focus, open, setOpen, width]);
  return { position, container: anchor.current?.closest('[aria-modal="true"]') ?? document.body };
}

interface OptionProps { value?: string | number; disabled?: boolean; children?: ReactNode }
const textOf = (node: ReactNode): string => Children.toArray(node).map(child => isValidElement<{ children?: ReactNode }>(child) ? textOf(child.props.children) : String(child)).join('');
interface SelectProps { label: string; value: string; onChange: (value: string) => void; children: ReactNode; disabled?: boolean; required?: boolean }

export function PickerSelect({ label, value, onChange, children, disabled, required }: SelectProps) {
  const id = useId(), labelId = `${id}-label`, listId = `${id}-list`;
  const trigger = useRef<HTMLButtonElement>(null), panel = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false), [active, setActive] = useState(0);
  const typed = useRef({ text: '', time: 0 });
  const options = Children.toArray(children).filter((child): child is React.ReactElement<OptionProps> => isValidElement<OptionProps>(child) && child.type === 'option')
    .map(child => ({ value: String(child.props.value ?? textOf(child.props.children)), label: textOf(child.props.children), disabled: !!child.props.disabled }));
  const selected = options.findIndex(option => option.value === value);
  const { position, container } = usePickerPopover(trigger, panel, trigger, open, setOpen);
  const choose = (index: number) => { if (!options[index] || options[index].disabled) return; onChange(options[index].value); setOpen(false); trigger.current?.focus(); };
  const show = () => { setActive(selected >= 0 ? selected : Math.max(0, options.findIndex(option => !option.disabled))); setOpen(true); };
  const move = (step: number) => {
    for (let n = 1; n <= options.length; n++) {
      const next = (active + step * n + options.length) % options.length;
      if (!options[next].disabled) { setActive(next); return; }
    }
  };
  const keyboard = (event: KeyboardEvent<HTMLButtonElement>) => {
    if (event.key === 'Tab') { setOpen(false); return; }
    if (['ArrowDown', 'ArrowUp', 'Home', 'End', 'Enter', ' '].includes(event.key)) {
      event.preventDefault();
      if (!open) { show(); return; }
      if (event.key === 'ArrowDown') move(1);
      else if (event.key === 'ArrowUp') move(-1);
      else if (event.key === 'Home') setActive(Math.max(0, options.findIndex(option => !option.disabled)));
      else if (event.key === 'End') setActive(options.reduce((last, option, index) => option.disabled ? last : index, -1));
      else choose(active);
    } else if (event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
      event.preventDefault();
      typed.current = { text: Date.now() - typed.current.time > 700 ? event.key : typed.current.text + event.key, time: Date.now() };
      const index = options.findIndex(option => !option.disabled && option.label.toLocaleLowerCase('pt-BR').startsWith(typed.current.text.toLocaleLowerCase('pt-BR')));
      if (index >= 0) { setActive(index); setOpen(true); }
    }
  };
  useEffect(() => { if (open) panel.current?.querySelector('[data-active="true"]')?.scrollIntoView({ block: 'nearest' }); }, [active, open]);
  return <div className="fl-picker-field">
    <label id={labelId} htmlFor={id}>{label}{required && <span aria-hidden="true"> *</span>}</label>
    <button id={id} ref={trigger} type="button" role="combobox" className="fl-picker-control fl-select-trigger" disabled={disabled}
      aria-labelledby={labelId} aria-describedby={`${id}-value`} aria-expanded={open} aria-controls={open ? listId : undefined} aria-haspopup="listbox" aria-required={required}
      aria-activedescendant={open && options[active] ? `${id}-option-${active}` : undefined}
      onClick={() => open ? setOpen(false) : show()} onKeyDown={keyboard}>
      <span id={`${id}-value`}>{options[selected]?.label || 'Selecione uma opção'}</span><ChevronDown size={17} aria-hidden="true" className={open ? 'fl-picker-chevron open' : 'fl-picker-chevron'} />
    </button>
    {open && createPortal(<div ref={panel} id={listId} role="listbox" aria-labelledby={labelId} className="fl-picker-popover fl-select-options" style={position}>
      {options.map((option, index) => <button key={`${option.value}-${index}`} id={`${id}-option-${index}`} type="button" role="option" tabIndex={-1}
        aria-selected={option.value === value} disabled={option.disabled} data-active={index === active} className="fl-select-option"
        onPointerMove={() => !option.disabled && setActive(index)} onMouseDown={event => event.preventDefault()} onClick={() => choose(index)}>
        <span>{option.label}</span>{option.value === value && <Check size={17} aria-hidden="true" />}
      </button>)}
      {options.length === 0 && <p className="fl-picker-empty">Nenhuma opção disponível.</p>}
    </div>, container)}
  </div>;
}

const iso = (date: Date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
function parse(value: string): Date | null {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return null;
  const date = new Date(`${value}T12:00:00`);
  return !Number.isNaN(date.getTime()) && iso(date) === value ? date : null;
}
const today = () => new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Cuiaba', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date());
const monthOf = (date: Date) => new Date(date.getFullYear(), date.getMonth(), 1, 12);
const monthLabel = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' });
const dayLabel = new Intl.DateTimeFormat('pt-BR', { day: 'numeric', month: 'long', year: 'numeric' });
interface DateProps { label: string; value: string; onChange: (value: string) => void; min?: string; max?: string; required?: boolean; disabled?: boolean }

export function CalendarField({ label, value, onChange, min, max, required, disabled }: DateProps) {
  const id = useId(), popupId = `${id}-calendar`, headingId = `${id}-month`;
  const anchor = useRef<HTMLDivElement>(null), input = useRef<HTMLInputElement>(null), panel = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false), [month, setMonth] = useState(() => monthOf(parse(value) ?? parse(today())!));
  const [focused, setFocused] = useState(value || today());
  const { position, container } = usePickerPopover(anchor, panel, input, open, setOpen, 320);
  const allowed = (date: string) => (!min || date >= min) && (!max || date <= max);
  const clamp = (date: string) => min && date < min ? min : max && date > max ? max : date;
  const show = () => {
    const date = parse(clamp(parse(value) ? value : today())) ?? parse(today())!;
    setFocused(iso(date)); setMonth(monthOf(date)); setOpen(true);
  };
  const choose = (date: string) => { if (date && !allowed(date)) return; onChange(date); setOpen(false); input.current?.focus(); };
  const changeMonth = (step: number) => {
    const next = new Date(month.getFullYear(), month.getMonth() + step, 1, 12);
    const target = iso(next);
    const date = parse(clamp(target))!;
    setMonth(monthOf(date)); setFocused(iso(date));
  };
  useLayoutEffect(() => { if (open) panel.current?.querySelector<HTMLButtonElement>(`[data-date="${focused}"]`)?.focus({ preventScroll: true }); }, [open, focused, month]);
  const keyboard = (event: KeyboardEvent<HTMLButtonElement>, date: string) => {
    const next = parse(date)!;
    const weekday = (next.getDay() + 6) % 7;
    if (event.key === 'ArrowLeft') next.setDate(next.getDate() - 1);
    else if (event.key === 'ArrowRight') next.setDate(next.getDate() + 1);
    else if (event.key === 'ArrowUp') next.setDate(next.getDate() - 7);
    else if (event.key === 'ArrowDown') next.setDate(next.getDate() + 7);
    else if (event.key === 'Home') next.setDate(next.getDate() - weekday);
    else if (event.key === 'End') next.setDate(next.getDate() + 6 - weekday);
    else if (event.key === 'PageUp' || event.key === 'PageDown') {
      const day = next.getDate(); next.setDate(1); next.setMonth(next.getMonth() + (event.key === 'PageUp' ? -1 : 1) * (event.shiftKey ? 12 : 1));
      next.setDate(Math.min(day, new Date(next.getFullYear(), next.getMonth() + 1, 0).getDate()));
    } else return;
    event.preventDefault();
    const target = parse(clamp(iso(next)))!; setFocused(iso(target)); setMonth(monthOf(target));
  };
  const first = new Date(month); first.setDate(1 - (month.getDay() + 6) % 7);
  const dates = Array.from({ length: 42 }, (_, index) => { const date = new Date(first); date.setDate(first.getDate() + index); return date; });
  const previousEnd = iso(new Date(month.getFullYear(), month.getMonth(), 0, 12));
  const nextStart = iso(new Date(month.getFullYear(), month.getMonth() + 1, 1, 12));
  return <div className="fl-picker-field">
    <label htmlFor={id}>{label}{required && <span aria-hidden="true"> *</span>}</label>
    <div ref={anchor} className="fl-calendar-input">
      <input ref={input} id={id} className="fl-picker-control" type="date" value={value} min={min} max={max} required={required} disabled={disabled}
        onChange={event => onChange(event.target.value)} onKeyDown={event => { if (event.altKey && event.key === 'ArrowDown') { event.preventDefault(); show(); } }} />
      <button type="button" className="fl-calendar-toggle" aria-label={`Calendário de ${label.toLocaleLowerCase('pt-BR')}`} disabled={disabled}
        aria-expanded={open} aria-haspopup="dialog" aria-controls={open ? popupId : undefined} onClick={() => open ? setOpen(false) : show()}>
        <CalendarDays size={18} aria-hidden="true" />
      </button>
    </div>
    {open && createPortal(<div ref={panel} id={popupId} role="dialog" aria-modal="false" aria-label={`Calendário: ${label}`} className="fl-picker-popover fl-calendar" style={position}>
      <div className="fl-calendar-header">
        <button type="button" aria-label="Mês anterior" disabled={!!min && previousEnd < min} onClick={() => changeMonth(-1)}><ChevronLeft size={18} aria-hidden="true" /></button>
        <strong id={headingId} aria-live="polite">{monthLabel.format(month)}</strong>
        <button type="button" aria-label="Próximo mês" disabled={!!max && nextStart > max} onClick={() => changeMonth(1)}><ChevronRight size={18} aria-hidden="true" /></button>
      </div>
      <div className="fl-calendar-weekdays" aria-hidden="true">{['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'].map(day => <span key={day}>{day}</span>)}</div>
      <div className="fl-calendar-days" role="group" aria-labelledby={headingId}>
        {dates.map(date => { const day = iso(date); return <button type="button" key={day} data-date={day} tabIndex={focused === day ? 0 : -1}
          className={`fl-calendar-day${date.getMonth() !== month.getMonth() ? ' outside' : ''}${day === value ? ' selected' : ''}${day === today() ? ' today' : ''}`}
          disabled={!allowed(day)} aria-label={dayLabel.format(date)} aria-pressed={day === value} aria-current={day === today() ? 'date' : undefined}
          onClick={() => choose(day)} onKeyDown={event => keyboard(event, day)}>{date.getDate()}</button>; })}
      </div>
      <div className="fl-calendar-footer"><button type="button" onClick={() => choose('')}>Limpar</button><button type="button" disabled={!allowed(today())} onClick={() => choose(today())}>Hoje</button></div>
    </div>, container)}
  </div>;
}

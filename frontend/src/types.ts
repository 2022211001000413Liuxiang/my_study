export type Status = 'learning' | 'reviewing' | 'done';
export type ReviewLevel = 'again' | 'normal' | 'easy';
export type ReviewFilter = 'all' | 'due' | 'unreviewed' | 'again' | 'normal' | 'easy' | 'scheduled';
export type ViewMode = 'reader' | 'review' | 'admin';

export type Heading = {
  level: number;
  text: string;
  slug: string;
};

export type Note = {
  id: string;
  path: string;
  title: string;
  category: string;
  tags: string[];
  headings: Heading[];
  updatedAt: string;
  wordCount: number;
  favorite: boolean;
  status: Status;
  reviewCount: number;
  lastReviewedAt: string | null;
  nextReviewAt: string | null;
  reviewLevel: ReviewLevel | null;
};

export type Category = {
  name: string;
  count: number;
};

export type Stats = {
  total: number;
  words: number;
  favorites: number;
  dueReviews: number;
  tags: Array<{ name: string; count: number }>;
};

export type NotesResponse = {
  notes: Note[];
  categories: Category[];
  stats: Stats;
};

export type ReviewQueue = {
  notes: Note[];
  dueCount: number;
  reviewedToday: number;
  nextReviewAt: string | null;
};

export type ReviewDay = {
  date: string;
  count: number;
};

export type ReviewOverview = {
  dueToday: number;
  reviewedToday: number;
  levels: {
    unreviewed: number;
    again: number;
    normal: number;
    easy: number;
    scheduled: number;
  };
  completedDays: ReviewDay[];
  upcomingDays: ReviewDay[];
  recentReviewed: Note[];
};

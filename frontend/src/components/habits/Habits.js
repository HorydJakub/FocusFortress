import React, { useEffect, useState, useRef } from 'react';
import api from '../../services/api';
import { Target, Plus, X, Trash2, Edit2, ChevronRight, FolderOpen, Smile, ArrowLeft, MoreVertical, CheckCircle, Flame, Layers, Info, Award, Trophy, Star } from 'lucide-react';
import EmojiPicker from 'emoji-picker-react';

const Habits = () => {
  const [categories, setCategories] = useState([]);
  const [subcategories, setSubcategories] = useState([]);
  const [habits, setHabits] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Navigation state
  const [currentView, setCurrentView] = useState('habits');
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [selectedSubcategory, setSelectedSubcategory] = useState(null);

  // Modal states
  const [showCategoryModal, setShowCategoryModal] = useState(false);
  const [showSubcategoryModal, setShowSubcategoryModal] = useState(false);
  const [showHabitModal, setShowHabitModal] = useState(false);
  const [editingCategory, setEditingCategory] = useState(null);
  const [editingSubcategory, setEditingSubcategory] = useState(null);
  const [editingHabit, setEditingHabit] = useState(null);

  // Form states
  const [newCategory, setNewCategory] = useState({ name: '', icon: '📚' });
  const [newSubcategory, setNewSubcategory] = useState({ name: '', icon: '📖', categoryId: null });
  const [newHabit, setNewHabit] = useState({
    name: '',
    description: '',
    categoryId: null,
    subcategoryId: null,
    icon: '🎯',
    durationDays: 21
  });
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);

  // Dropdown menu state
  const [openDropdown, setOpenDropdown] = useState(null);
  const dropdownRef = useRef(null);

  // Track which habits were marked done today - with localStorage
  const [markedDoneToday, setMarkedDoneToday] = useState(() => {
    try {
      const stored = localStorage.getItem('markedDoneToday');
      if (stored) {
        const { date, habitIds } = JSON.parse(stored);
        const today = new Date().toDateString();
        if (date === today) {
          return new Set(habitIds);
        }
      }
    } catch (e) {
      console.error('Failed to load markedDoneToday from localStorage:', e);
    }
    return new Set();
  });

  const [availableSubcategories, setAvailableSubcategories] = useState([]);
  const [canAddHabit, setCanAddHabit] = useState(false);
  const [categoriesWithSubcategories, setCategoriesWithSubcategories] = useState([]);
  const [allSubcategories, setAllSubcategories] = useState([]);

  // Celebration state for completed habits
  const [showCelebration, setShowCelebration] = useState(false);
  const [completedHabitName, setCompletedHabitName] = useState('');

  // Save markedDoneToday to localStorage whenever it changes
  useEffect(() => {
    try {
      const today = new Date().toDateString();
      const data = {
        date: today,
        habitIds: Array.from(markedDoneToday)
      };
      localStorage.setItem('markedDoneToday', JSON.stringify(data));
    } catch (e) {
      console.error('Failed to save markedDoneToday to localStorage:', e);
    }
  }, [markedDoneToday]);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setOpenDropdown(null);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    fetchCategories();
    fetchHabits();
  }, []);

  useEffect(() => {
    if (selectedCategory) {
      fetchSubcategories(selectedCategory.id);
    }
  }, [selectedCategory]);

  // Fetch subcategories when category is selected in habit modal
  useEffect(() => {
    const fetchSubcategoriesForHabit = async () => {
      if (newHabit.categoryId || editingHabit?.categoryId) {
        const categoryId = newHabit.categoryId || editingHabit?.categoryId;
        try {
          const treeRes = await api.get('/habits/tree');
          const categoryTree = treeRes.data.find(c => c.id === categoryId);
          setAvailableSubcategories(categoryTree?.subcategories || []);
        } catch (e) {
          console.error('Failed to fetch subcategories for habit:', e);
          setAvailableSubcategories([]);
        }
      } else {
        setAvailableSubcategories([]);
      }
    };

    fetchSubcategoriesForHabit();
  }, [newHabit.categoryId, editingHabit?.categoryId]);

  const fetchCategories = async () => {
    try {
      const res = await api.get('/categories');
      setCategories(res.data || []);
    } catch (e) {
      console.error('Failed to fetch categories:', e);
    }
  };

  const fetchSubcategories = async (categoryId) => {
    try {
      const treeRes = await api.get('/habits/tree');
      const categoryTree = treeRes.data.find(c => c.id === categoryId);
      setSubcategories(categoryTree?.subcategories || []);
    } catch (e) {
      console.error('Failed to fetch subcategories:', e);
      setSubcategories([]);
    }
  };

  const fetchHabits = async () => {
    setLoading(true);
    try {
      const res = await api.get('/habits');
      setHabits(res.data || []);
      setError(null);
    } catch (e) {
      setError('Failed to load habits');
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleEmojiClick = (emojiData) => {
    if (editingCategory) {
      setEditingCategory({ ...editingCategory, icon: emojiData.emoji });
    } else if (editingSubcategory) {
      setEditingSubcategory({ ...editingSubcategory, icon: emojiData.emoji });
    } else if (editingHabit) {
      setEditingHabit({ ...editingHabit, icon: emojiData.emoji });
    } else if (showCategoryModal) {
      setNewCategory({ ...newCategory, icon: emojiData.emoji });
    } else if (showSubcategoryModal) {
      setNewSubcategory({ ...newSubcategory, icon: emojiData.emoji });
    } else if (showHabitModal) {
      setNewHabit({ ...newHabit, icon: emojiData.emoji });
    }
    setShowEmojiPicker(false);
  };

  // Category CRUD operations
  const handleCreateCategory = async () => {
    try {
      if (!newCategory.name.trim()) {
        alert('Please provide a category name');
        return;
      }

      await api.post('/categories', {
        name: newCategory.name,
        icon: newCategory.icon
      });

      await fetchCategories();
      setShowCategoryModal(false);
      resetCategoryForm();
    } catch (e) {
      console.error('Failed to create category:', e);
      alert(e.response?.data?.message || 'Failed to create category');
    }
  };

  const handleEditCategory = async () => {
    try {
      if (!editingCategory.name.trim()) {
        alert('Please provide a category name');
        return;
      }

      await api.put(`/categories/${editingCategory.id}`, {
        name: editingCategory.name,
        icon: editingCategory.icon
      });

      await fetchCategories();
      await fetchHabits();
      setEditingCategory(null);
      setShowEmojiPicker(false);
    } catch (e) {
      console.error('Failed to update category:', e);
      alert(e.response?.data?.message || 'Failed to update category');
    }
  };

  const handleDeleteCategory = async (categoryId) => {
    try {
      const treeRes = await api.get('/habits/tree');
      const categoryTree = treeRes.data.find(c => c.id === categoryId);

      if (categoryTree?.subcategories?.length > 0) {
        alert('Cannot delete category with subcategories. Please delete all subcategories first.');
        return;
      }

      if (!window.confirm('Are you sure you want to delete this category?')) {
        return;
      }

      await api.delete(`/categories/${categoryId}`);
      await fetchCategories();
    } catch (e) {
      console.error('Failed to delete category:', e);
      alert(e.response?.data?.message || 'Failed to delete category');
    }
  };

  // Subcategory CRUD operations
  const handleCreateSubcategory = async () => {
    try {
      if (!newSubcategory.name.trim()) {
        alert('Please provide a subcategory name');
        return;
      }

      await api.post('/subcategories', {
        name: newSubcategory.name,
        icon: newSubcategory.icon,
        categoryId: selectedCategory.id
      });

      await fetchSubcategories(selectedCategory.id);
      await fetchCategories();
      setShowSubcategoryModal(false);
      resetSubcategoryForm();
    } catch (e) {
      console.error('Failed to create subcategory:', e);
      alert(e.response?.data?.message || 'Failed to create subcategory');
    }
  };

  const handleEditSubcategory = async () => {
    try {
      if (!editingSubcategory.name.trim()) {
        alert('Please provide a subcategory name');
        return;
      }

      await api.put(`/subcategories/${editingSubcategory.id}`, {
        name: editingSubcategory.name,
        icon: editingSubcategory.icon,
        categoryId: selectedCategory.id
      });

      await fetchSubcategories(selectedCategory.id);
      await fetchHabits();
      setEditingSubcategory(null);
      setShowEmojiPicker(false);
    } catch (e) {
      console.error('Failed to update subcategory:', e);
      alert(e.response?.data?.message || 'Failed to update subcategory');
    }
  };

  const handleDeleteSubcategory = async (subcategoryId) => {
    try {
      const treeRes = await api.get('/habits/tree');
      const categoryTree = treeRes.data.find(c => c.id === selectedCategory.id);
      const subcategory = categoryTree?.subcategories?.find(s => s.id === subcategoryId);

      if (subcategory?.habits?.length > 0) {
        alert('Cannot delete subcategory with habits. Please delete all habits first.');
        return;
      }

      if (!window.confirm('Are you sure you want to delete this subcategory?')) {
        return;
      }

      await api.delete(`/subcategories/${subcategoryId}`);
      await fetchSubcategories(selectedCategory.id);
    } catch (e) {
      console.error('Failed to delete subcategory:', e);
      alert(e.response?.data?.message || 'Failed to delete subcategory');
    }
  };

  // Habit CRUD operations
  const handleCreateHabit = async () => {
    try {
      if (!newHabit.name.trim()) {
        alert('Please provide a habit name');
        return;
      }
      if (!newHabit.categoryId) {
        alert('Please select a category');
        return;
      }
      if (!newHabit.subcategoryId) {
        alert('Please select a subcategory');
        return;
      }

      await api.post('/habits', {
        name: newHabit.name,
        description: newHabit.description,
        categoryId: newHabit.categoryId,
        subcategoryId: newHabit.subcategoryId,
        icon: newHabit.icon,
        durationDays: newHabit.durationDays
      });

      await fetchHabits();
      setShowHabitModal(false);
      resetHabitForm();
    } catch (e) {
      console.error('Failed to create habit:', e);
      alert(e.response?.data?.message || 'Failed to create habit');
    }
  };

  const handleEditHabit = async () => {
    try {
      if (!editingHabit.name.trim()) {
        alert('Please provide a habit name');
        return;
      }
      if (!editingHabit.categoryId) {
        alert('Please select a category');
        return;
      }
      if (!editingHabit.subcategoryId) {
        alert('Please select a subcategory');
        return;
      }

      await api.put(`/habits/${editingHabit.id}`, {
        name: editingHabit.name,
        description: editingHabit.description,
        categoryId: editingHabit.categoryId,
        subcategoryId: editingHabit.subcategoryId,
        icon: editingHabit.icon,
        durationDays: editingHabit.durationDays
      });

      await fetchHabits();
      setEditingHabit(null);
      setShowEmojiPicker(false);
      setAvailableSubcategories([]);
    } catch (e) {
      console.error('Failed to update habit:', e);
      alert(e.response?.data?.message || 'Failed to update habit');
    }
  };

  const handleDeleteHabit = async (habitId) => {
    const habit = habits.find(h => h.id === habitId);
    const confirmMessage = habit?.done
      ? 'Are you sure you want to delete this completed habit? This will permanently remove all progress.'
      : 'Are you sure you want to delete this habit? This will also delete all progress.';

    if (!window.confirm(confirmMessage)) {
      return;
    }

    try {
      await api.delete(`/habits/${habitId}`);
      await fetchHabits();
      setMarkedDoneToday(prev => {
        const newSet = new Set(prev);
        newSet.delete(habitId);
        return newSet;
      });
    } catch (e) {
      console.error('Failed to delete habit:', e);
      alert(e.response?.data?.message || 'Failed to delete habit');
    }
  };

  const handleMarkDone = async (habitId) => {
    try {
      const habit = habits.find(h => h.id === habitId);
      const response = await api.post(`/habits/${habitId}/done`);
      const newStreak = response.data;

      setMarkedDoneToday(prev => new Set([...prev, habitId]));

      // Check if habit was just completed
      if (habit && newStreak >= habit.durationDays) {
        setCompletedHabitName(habit.name);
        setShowCelebration(true);
        setTimeout(() => setShowCelebration(false), 5000);
      }

      await fetchHabits(); // Refresh to update done status
    } catch (e) {
      console.error('Failed to mark habit as done:', e);
      if (e.response?.data?.message?.includes('Already marked') ||
          e.response?.data?.message?.includes('already marked') ||
          e.response?.data?.message?.includes('already completed')) {
        setMarkedDoneToday(prev => new Set([...prev, habitId]));
      } else {
        alert(e.response?.data?.message || 'Failed to mark habit as done');
      }
    }
  };

  // Navigation helpers
  const navigateToCategory = (category) => {
    setSelectedCategory(category);
    setCurrentView('subcategories');
    setOpenDropdown(null);
  };

  const navigateToSubcategory = (subcategory) => {
    setSelectedSubcategory(subcategory);
    setCurrentView('subcategoryHabits');
    setOpenDropdown(null);
  };

  const navigateBack = () => {
    if (currentView === 'subcategoryHabits') {
      setSelectedSubcategory(null);
      setCurrentView('subcategories');
    } else if (currentView === 'subcategories') {
      setSelectedCategory(null);
      setSubcategories([]);
      setCurrentView('categories');
    } else if (currentView === 'categories') {
      setCurrentView('habits');
    }
    setOpenDropdown(null);
  };

  const resetCategoryForm = () => {
    setNewCategory({ name: '', icon: '📚' });
    setShowEmojiPicker(false);
  };

  const resetSubcategoryForm = () => {
    setNewSubcategory({ name: '', icon: '📖', categoryId: null });
    setShowEmojiPicker(false);
  };

  const resetHabitForm = () => {
    setNewHabit({
      name: '',
      description: '',
      categoryId: null,
      subcategoryId: null,
      icon: '🎯',
      durationDays: 21
    });
    setShowEmojiPicker(false);
    setAvailableSubcategories([]);
  };

  const openEditCategoryModal = (category) => {
    setEditingCategory({ ...category });
    setShowEmojiPicker(false);
  };

  const openEditSubcategoryModal = (subcategory) => {
    setEditingSubcategory({ ...subcategory });
    setShowEmojiPicker(false);
  };

  const openEditHabitModal = (habit) => {
    if (habit.done) {
      alert('Cannot edit completed habits. You can only delete them.');
      return;
    }
    setEditingHabit({ ...habit });
    setShowEmojiPicker(false);
  };

  // Check if user can create habits on mount and when categories change
  useEffect(() => {
    const checkCanCreate = async () => {
      try {
        const treeRes = await api.get('/habits/tree');
        const catsWithSubs = treeRes.data.filter(
          category => category.subcategories && category.subcategories.length > 0
        );
        setCategoriesWithSubcategories(catsWithSubs);
        setCanAddHabit(catsWithSubs.length > 0);

        const allSubs = treeRes.data.flatMap(category =>
          category.subcategories || []
        );
        setAllSubcategories(allSubs);
      } catch (e) {
        console.error('Failed to check if can create habits:', e);
        setCanAddHabit(false);
        setCategoriesWithSubcategories([]);
        setAllSubcategories([]);
      }
    };
    checkCanCreate();
  }, [categories, habits]);

  // Get category name by id
  const getCategoryName = (categoryId) => {
    const category = categories.find(c => c.id === categoryId);
    return category ? `${category.icon || '📁'} ${category.name}` : 'Unknown';
  };

  // Get subcategory name by id
  const getSubcategoryName = (subcategoryId) => {
    const subcategory = allSubcategories.find(s => s.id === subcategoryId);
    return subcategory ? `${subcategory.icon || '📂'} ${subcategory.name}` : 'Unknown';
  };

  // Sort habits by category, then subcategory, then name
  const sortedHabits = [...habits].sort((a, b) => {
    const catA = categories.find(c => c.id === a.categoryId)?.name || '';
    const catB = categories.find(c => c.id === b.categoryId)?.name || '';
    if (catA !== catB) return catA.localeCompare(catB);

    if (a.subcategoryId !== b.subcategoryId) {
      return (a.subcategoryId || 0) - (b.subcategoryId || 0);
    }

    return a.name.localeCompare(b.name);
  });

  const inProgressHabits = sortedHabits.filter(h => !h.done);
  const completedHabits = sortedHabits.filter(h => h.done);

  // Celebration Modal for Completed Habits
  const renderCelebrationModal = () => {
    if (!showCelebration) return null;

    return (
      <div style={{
        position: 'fixed',
        top: '20px',
        right: '20px',
        zIndex: 2000,
        animation: 'slideIn 0.5s ease-out'
      }}>
        <div style={{
          background: 'linear-gradient(135deg, #FFD700 0%, #FFA500 100%)',
          padding: '24px 32px',
          borderRadius: '16px',
          boxShadow: '0 8px 32px rgba(255, 165, 0, 0.4)',
          display: 'flex',
          alignItems: 'center',
          gap: '16px',
          maxWidth: '400px'
        }}>
          <Trophy size={48} color="white" style={{ flexShrink: 0 }} />
          <div>
            <h3 style={{ margin: '0 0 8px 0', color: 'white', fontSize: '20px', fontWeight: 'bold' }}>
              🎉 Congratulations!
            </h3>
            <p style={{ margin: 0, color: 'white', fontSize: '16px' }}>
              You've completed <strong>"{completedHabitName}"</strong>!
            </p>
          </div>
        </div>
      </div>
    );
  };

  // Render breadcrumb navigation
  const renderBreadcrumb = () => {
    if (currentView === 'habits') return null;

    return (
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        marginBottom: '24px',
        fontSize: '14px',
        color: '#666'
      }}>
        <span
          onClick={() => {
            setSelectedCategory(null);
            setSelectedSubcategory(null);
            setCurrentView('habits');
            setOpenDropdown(null);
          }}
          style={{
            cursor: 'pointer',
            transition: 'color 0.2s',
            fontWeight: '500'
          }}
          onMouseEnter={(e) => e.target.style.color = '#ff6b35'}
          onMouseLeave={(e) => e.target.style.color = '#666'}
        >
          My Habits
        </span>

        <ChevronRight size={16} />

        <span
          onClick={() => {
            setSelectedCategory(null);
            setSelectedSubcategory(null);
            setCurrentView('categories');
            setOpenDropdown(null);
          }}
          style={{
            cursor: 'pointer',
            transition: 'color 0.2s',
            fontWeight: '500'
          }}
          onMouseEnter={(e) => e.target.style.color = '#ff6b35'}
          onMouseLeave={(e) => e.target.style.color = '#666'}
        >
          All Categories
        </span>

        {selectedCategory && (
          <>
            <ChevronRight size={16} />
            <span
              onClick={() => {
                if (currentView === 'subcategoryHabits') {
                  navigateBack();
                }
              }}
              style={{
                cursor: currentView === 'subcategoryHabits' ? 'pointer' : 'default',
                transition: 'color 0.2s',
                fontWeight: '500',
                color: currentView === 'subcategoryHabits' ? '#666' : '#ff6b35'
              }}
              onMouseEnter={(e) => currentView === 'subcategoryHabits' && (e.target.style.color = '#ff6b35')}
              onMouseLeave={(e) => currentView === 'subcategoryHabits' && (e.target.style.color = '#666')}
            >
              {selectedCategory.icon} {selectedCategory.name}
            </span>
          </>
        )}

        {selectedSubcategory && (
          <>
            <ChevronRight size={16} />
            <span style={{ fontWeight: '500', color: '#ff6b35' }}>
              {selectedSubcategory.icon} {selectedSubcategory.name}
            </span>
          </>
        )}
      </div>
    );
  };

  // Render empty state
  const renderEmptyState = (type) => {
    const messages = {
      habits: canAddHabit ? {
        icon: <Target size={64} color="#ff6b35" style={{ opacity: 0.3, marginBottom: '16px' }} />,
        title: "It's a bit empty here!",
        subtitle: "Start by creating your first habit to track your progress",
        buttons: [
          {
            text: "Add Habit",
            action: () => setShowHabitModal(true),
            primary: true
          },
          {
            text: "Browse Categories",
            action: () => setCurrentView('categories'),
            primary: false
          }
        ]
      } : {
        icon: <Layers size={64} color="#ff6b35" style={{ opacity: 0.3, marginBottom: '16px' }} />,
        title: "You need to set up categories first!",
        subtitle: "Before creating habits, you need at least one category with subcategories",
        buttons: [
          {
            text: "Manage Categories",
            action: () => setCurrentView('categories'),
            primary: true
          }
        ]
      },
      categories: {
        icon: <Target size={64} color="#ff6b35" style={{ opacity: 0.3, marginBottom: '16px' }} />,
        title: "It's a bit empty here!",
        subtitle: "Start by creating your first category to organize your habits",
        buttons: [
          {
            text: "Create Category",
            action: () => setShowCategoryModal(true),
            primary: true
          }
        ]
      },
      subcategories: {
        icon: <FolderOpen size={64} color="#ff6b35" style={{ opacity: 0.3, marginBottom: '16px' }} />,
        title: "It's a bit empty here!",
        subtitle: `Add subcategories to organize habits within "${selectedCategory?.name}"`,
        buttons: [
          {
            text: "Create Subcategory",
            action: () => setShowSubcategoryModal(true),
            primary: true
          }
        ]
      }
    };

    const config = messages[type];

    return (
      <div style={{
        padding: '60px 40px',
        textAlign: 'center',
        background: 'white',
        borderRadius: '16px',
        boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
      }}>
        {config.icon}
        <h2 style={{ color: '#333', marginBottom: '8px', fontSize: '24px' }}>{config.title}</h2>
        <p style={{ margin: '0 0 24px 0', color: '#666' }}>{config.subtitle}</p>
        <div style={{ display: 'flex', gap: '12px', justifyContent: 'center', flexWrap: 'wrap' }}>
          {config.buttons.map((btn, idx) => (
            <button
              key={idx}
              onClick={btn.action}
              style={{
                padding: '12px 32px',
                background: btn.primary ?
                  'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)' :
                  'white',
                color: btn.primary ? 'white' : '#ff6b35',
                border: btn.primary ? 'none' : '2px solid #ff6b35',
                borderRadius: '12px',
                fontSize: '16px',
                fontWeight: 'bold',
                cursor: 'pointer',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '8px',
                boxShadow: btn.primary ? '0 4px 12px rgba(255, 107, 53, 0.3)' : 'none',
                transition: 'transform 0.2s, box-shadow 0.2s'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'translateY(-2px)';
                if (btn.primary) {
                  e.currentTarget.style.boxShadow = '0 6px 16px rgba(255, 107, 53, 0.4)';
                } else {
                  e.currentTarget.style.background = '#fff5eb';
                }
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'translateY(0)';
                if (btn.primary) {
                  e.currentTarget.style.boxShadow = '0 4px 12px rgba(255, 107, 53, 0.3)';
                } else {
                  e.currentTarget.style.background = 'white';
                }
              }}
            >
              <Plus size={20} />
              {btn.text}
            </button>
          ))}
        </div>
      </div>
    );
  };

  // Render habits table
  const renderHabitsTable = (habitsToRender, isCompleted = false) => {
    if (habitsToRender.length === 0) return null;

    return (
      <div style={{
        background: 'white',
        borderRadius: '16px',
        boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
        marginBottom: '32px'
      }}>
        {/* Table Header */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: isCompleted ? '2fr 1.5fr 1.5fr 1fr 1.5fr 80px' : '2fr 1.5fr 1.5fr 1fr 1.5fr 1.5fr 80px',
          gap: '16px',
          padding: '20px 24px',
          background: isCompleted ? '#f0f7f0' : '#f8f8f8',
          borderBottom: '2px solid #e0e0e0',
          fontWeight: '600',
          fontSize: '14px',
          color: '#666',
          borderTopLeftRadius: '16px',
          borderTopRightRadius: '16px'
        }}>
          <div>HABIT</div>
          <div>CATEGORY</div>
          <div>SUBCATEGORY</div>
          <div style={{ textAlign: 'center' }}>STREAK</div>
          <div style={{ textAlign: 'center' }}>PROGRESS</div>
          {!isCompleted && <div style={{ textAlign: 'center' }}>MARK DONE</div>}
          <div></div>
        </div>

        {/* Table Rows */}
        {habitsToRender.map((habit) => {
          const progress = Math.min((habit.currentStreak / habit.durationDays) * 100, 100);

          return (
            <div
              key={habit.id}
              style={{
                display: 'grid',
                gridTemplateColumns: isCompleted ? '2fr 1.5fr 1.5fr 1fr 1.5fr 80px' : '2fr 1.5fr 1.5fr 1fr 1.5fr 1.5fr 80px',
                gap: '16px',
                padding: '20px 24px',
                borderBottom: '1px solid #f0f0f0',
                alignItems: 'center',
                transition: 'background 0.2s',
                background: isCompleted ? '#fafffe' : 'white'
              }}
              onMouseEnter={(e) => e.currentTarget.style.background = isCompleted ? '#f0f7f0' : '#fafafa'}
              onMouseLeave={(e) => e.currentTarget.style.background = isCompleted ? '#fafffe' : 'white'}
            >
              {/* Habit Name with Icon */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <div style={{
                  fontSize: '24px',
                  flexShrink: 0,
                  lineHeight: 1,
                  display: 'flex',
                  alignItems: 'center'
                }}>
                  {habit.icon || '🎯'}
                </div>
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  flex: 1
                }}>
                  <span style={{
                    fontWeight: '600',
                    color: '#333'
                  }}>
                    {habit.name}
                  </span>
                  {isCompleted && (
                    <Trophy
                      size={18}
                      color="#FFD700"
                      style={{ flexShrink: 0, display: 'block' }}
                    />
                  )}
                  {habit.description && (
                    <div
                      className="habit-info-wrapper"
                      style={{
                        position: 'relative',
                        display: 'inline-flex',
                        alignItems: 'center',
                        flexShrink: 0
                      }}
                      onMouseEnter={(e) => {
                        const tooltip = e.currentTarget.querySelector('.habit-tooltip');
                        if (tooltip) tooltip.style.opacity = '1';
                      }}
                      onMouseLeave={(e) => {
                        const tooltip = e.currentTarget.querySelector('.habit-tooltip');
                        if (tooltip) tooltip.style.opacity = '0';
                      }}
                    >
                      <Info
                        size={18}
                        style={{
                          color: '#ff6b35',
                          cursor: 'pointer',
                          flexShrink: 0,
                          display: 'block'
                        }}
                      />
                      <div className="habit-tooltip" style={{
                        position: 'absolute', bottom: '100%', left: '50%',
                        transform: 'translateX(-50%)', marginBottom: '8px',
                        padding: '10px 14px', background: '#333', color: 'white',
                        borderRadius: '8px', fontSize: '13px', maxWidth: '400px',
                        minWidth: '200px', whiteSpace: 'normal', wordWrap: 'break-word',
                        lineHeight: '1.5', opacity: 0, pointerEvents: 'none',
                        transition: 'opacity 0.2s', zIndex: 10000,
                        boxShadow: '0 4px 12px rgba(0,0,0,0.15)'
                      }}>
                        {habit.description}
                        <div style={{
                          position: 'absolute', top: '100%', left: '50%',
                          transform: 'translateX(-50%)', width: 0, height: 0,
                          borderLeft: '6px solid transparent', borderRight: '6px solid transparent',
                          borderTop: '6px solid #333'
                        }} />
                      </div>
                    </div>
                  )}
                </div>
              </div>

              {/* Category */}
              <div style={{ color: '#666', fontSize: '14px' }}>
                {getCategoryName(habit.categoryId)}
              </div>

              {/* Subcategory */}
              <div style={{ color: '#666', fontSize: '14px' }}>
                {getSubcategoryName(habit.subcategoryId)}
              </div>

              {/* Streak */}
              <div style={{ textAlign: 'center' }}>
                <div style={{
                  display: 'inline-flex', alignItems: 'center', gap: '6px',
                  padding: '6px 12px',
                  background: isCompleted ? 'linear-gradient(135deg, #FFD700 0%, #FFA500 100%)' :
                             habit.currentStreak > 0 ? '#fff5eb' : '#f8f8f8',
                  borderRadius: '20px', fontSize: '14px', fontWeight: '600',
                  color: isCompleted ? 'white' : habit.currentStreak > 0 ? '#ff6b35' : '#999'
                }}>
                  {isCompleted ? <Star size={16} /> : habit.currentStreak > 0 && <Flame size={16} />}
                  {habit.currentStreak} {habit.currentStreak === 1 ? 'day' : 'days'}
                </div>
              </div>

              {/* Progress Bar */}
              <div style={{ textAlign: 'center', position: 'relative' }}>
                <div style={{
                  position: 'relative', width: '100%', height: '24px',
                  background: '#f0f0f0', borderRadius: '14px', overflow: 'hidden',
                  boxShadow: 'inset 0 2px 4px rgba(0,0,0,0.1)', cursor: 'pointer'
                }}
                onMouseEnter={(e) => {
                  const tooltip = e.currentTarget.nextElementSibling;
                  if (tooltip) tooltip.style.opacity = '1';
                }}
                onMouseLeave={(e) => {
                  const tooltip = e.currentTarget.nextElementSibling;
                  if (tooltip) tooltip.style.opacity = '0';
                }}>
                  <div style={{
                    position: 'absolute', top: 0, left: 0, height: '100%',
                    width: `${progress}%`,
                    background: progress === 100 || isCompleted
                      ? 'linear-gradient(135deg, #FFD700 0%, #FFA500 100%)'
                      : 'linear-gradient(135deg, #4CAF50 0%, #45a049 100%)',
                    transition: 'width 0.3s ease', borderRadius: '14px'
                  }} />
                  <div style={{
                    position: 'absolute', top: '50%', left: '50%',
                    transform: 'translate(-50%, -50%)', fontSize: '13px',
                    fontWeight: '700',
                    color: progress > 50 ? 'white' : '#333',
                    textShadow: progress > 50 ? '0 1px 2px rgba(0,0,0,0.2)' : 'none',
                    zIndex: 1
                  }}>
                    {progress.toFixed(0)}%
                  </div>
                </div>
                <div style={{
                  position: 'absolute', bottom: '100%', left: '50%',
                  transform: 'translateX(-50%)', marginBottom: '8px',
                  padding: '8px 12px', background: '#333', color: 'white',
                  borderRadius: '8px', fontSize: '13px', whiteSpace: 'nowrap',
                  opacity: 0, pointerEvents: 'none', transition: 'opacity 0.2s',
                  zIndex: 10000, boxShadow: '0 4px 12px rgba(0,0,0,0.15)'
                }}>
                  {habit.currentStreak}/{habit.durationDays} days completed
                  {isCompleted && ' ✓'}
                  <div style={{
                    position: 'absolute', top: '100%', left: '50%',
                    transform: 'translateX(-50%)', width: 0, height: 0,
                    borderLeft: '6px solid transparent', borderRight: '6px solid transparent',
                    borderTop: '6px solid #333'
                  }} />
                </div>
              </div>

              {/* Mark Done Button (only for in-progress habits) */}
              {!isCompleted && (
                <div style={{ textAlign: 'center' }}>
                  <button
                    onClick={() => handleMarkDone(habit.id)}
                    disabled={markedDoneToday.has(habit.id)}
                    style={{
                      padding: '10px 20px',
                      background: markedDoneToday.has(habit.id) ?
                        '#e0e0e0' :
                        'linear-gradient(135deg, #4CAF50 0%, #45a049 100%)',
                      color: markedDoneToday.has(habit.id) ? '#999' : 'white',
                      border: 'none', borderRadius: '10px', fontSize: '14px',
                      fontWeight: '600',
                      cursor: markedDoneToday.has(habit.id) ? 'not-allowed' : 'pointer',
                      display: 'inline-flex', alignItems: 'center', gap: '6px',
                      boxShadow: markedDoneToday.has(habit.id) ?
                        'none' : '0 4px 12px rgba(76, 175, 80, 0.3)',
                      transition: 'transform 0.2s, box-shadow 0.2s',
                      opacity: markedDoneToday.has(habit.id) ? 0.6 : 1
                    }}
                    onMouseEnter={(e) => {
                      if (!markedDoneToday.has(habit.id)) {
                        e.currentTarget.style.transform = 'translateY(-2px)';
                        e.currentTarget.style.boxShadow = '0 6px 16px rgba(76, 175, 80, 0.4)';
                      }
                    }}
                    onMouseLeave={(e) => {
                      if (!markedDoneToday.has(habit.id)) {
                        e.currentTarget.style.transform = 'translateY(0)';
                        e.currentTarget.style.boxShadow = '0 4px 12px rgba(76, 175, 80, 0.3)';
                      }
                    }}
                  >
                    <CheckCircle size={16} />
                    {markedDoneToday.has(habit.id) ? 'Done!' : 'Mark Done'}
                  </button>
                </div>
              )}

              {/* Three-dot menu */}
              <div style={{ position: 'relative', textAlign: 'center' }}>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setOpenDropdown(openDropdown === `habit-${habit.id}` ? null : `habit-${habit.id}`);
                  }}
                  style={{
                    padding: '6px', background: 'transparent', border: 'none',
                    borderRadius: '8px', color: '#666', cursor: 'pointer',
                    display: 'inline-flex', alignItems: 'center',
                    justifyContent: 'center', transition: 'all 0.2s'
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.background = '#f0f0f0';
                    e.currentTarget.style.color = '#333';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.background = 'transparent';
                    e.currentTarget.style.color = '#666';
                  }}
                >
                  <MoreVertical size={20} />
                </button>

                {/* Dropdown menu */}
                {openDropdown === `habit-${habit.id}` && (
                  <div
                    ref={dropdownRef}
                    style={{
                      position: 'absolute', top: '36px', right: '0',
                      background: 'white', borderRadius: '12px',
                      boxShadow: '0 4px 16px rgba(0,0,0,0.15)',
                      minWidth: '140px', zIndex: 1000
                    }}
                    onClick={(e) => e.stopPropagation()}
                  >
                    {!isCompleted && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          openEditHabitModal(habit);
                          setOpenDropdown(null);
                        }}
                        style={{
                          width: '100%', padding: '12px 16px',
                          background: 'white', border: 'none', cursor: 'pointer',
                          display: 'flex', alignItems: 'center', gap: '10px',
                          fontSize: '14px', fontWeight: '500', color: '#333',
                          transition: 'background 0.2s'
                        }}
                        onMouseEnter={(e) => e.currentTarget.style.background = '#f8f8f8'}
                        onMouseLeave={(e) => e.currentTarget.style.background = 'white'}
                      >
                        <Edit2 size={16} color="#4CAF50" />
                        Edit
                      </button>
                    )}
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDeleteHabit(habit.id);
                        setOpenDropdown(null);
                      }}
                      style={{
                        width: '100%', padding: '12px 16px',
                        background: 'white', border: 'none', cursor: 'pointer',
                        display: 'flex', alignItems: 'center', gap: '10px',
                        fontSize: '14px', fontWeight: '500', color: '#333',
                        transition: 'background 0.2s'
                      }}
                      onMouseEnter={(e) => e.currentTarget.style.background = '#f8f8f8'}
                      onMouseLeave={(e) => e.currentTarget.style.background = 'white'}
                    >
                      <Trash2 size={16} color="#dc3545" />
                      Delete
                    </button>
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>
    );
  };


  // Render category card
  const renderCategoryCard = (category) => (
    <div
      key={category.id}
      style={{
        background: 'white',
        borderRadius: '16px',
        padding: '24px',
        boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
        transition: 'transform 0.2s, box-shadow 0.2s',
        cursor: 'pointer',
        position: 'relative',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '12px',
        minHeight: '180px'
      }}
      onClick={() => navigateToCategory(category)}
      onMouseEnter={(e) => {
        e.currentTarget.style.transform = 'translateY(-4px)';
        e.currentTarget.style.boxShadow = '0 8px 24px rgba(255, 107, 53, 0.2)';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.transform = 'translateY(0)';
        e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.1)';
      }}
    >
      <div style={{ position: 'absolute', top: '12px', right: '12px' }}>
        <button
          onClick={(e) => {
            e.stopPropagation();
            setOpenDropdown(openDropdown === `category-${category.id}` ? null : `category-${category.id}`);
          }}
          style={{
            padding: '6px', background: 'transparent', border: 'none',
            borderRadius: '8px', color: '#666', cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            transition: 'all 0.2s'
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.background = '#f0f0f0';
            e.currentTarget.style.color = '#333';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.background = 'transparent';
            e.currentTarget.style.color = '#666';
          }}
        >
          <MoreVertical size={20} />
        </button>

        {openDropdown === `category-${category.id}` && (
          <div
            ref={dropdownRef}
            style={{
              position: 'absolute', top: '36px', right: '0',
              background: 'white', borderRadius: '12px',
              boxShadow: '0 4px 16px rgba(0,0,0,0.15)',
              minWidth: '140px', zIndex: 1000
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <button
              onClick={(e) => {
                e.stopPropagation();
                openEditCategoryModal(category);
                setOpenDropdown(null);
              }}
              style={{
                width: '100%', padding: '12px 16px',
                background: 'white', border: 'none', cursor: 'pointer',
                display: 'flex', alignItems: 'center', gap: '10px',
                fontSize: '14px', fontWeight: '500', color: '#333',
                transition: 'background 0.2s'
              }}
              onMouseEnter={(e) => e.currentTarget.style.background = '#f8f8f8'}
              onMouseLeave={(e) => e.currentTarget.style.background = 'white'}
            >
              <Edit2 size={16} color="#4CAF50" />
              Edit
            </button>
            <button
              onClick={(e) => {
                e.stopPropagation();
                handleDeleteCategory(category.id);
                setOpenDropdown(null);
              }}
              style={{
                width: '100%', padding: '12px 16px',
                background: 'white', border: 'none', cursor: 'pointer',
                display: 'flex', alignItems: 'center', gap: '10px',
                fontSize: '14px', fontWeight: '500', color: '#333',
                transition: 'background 0.2s'
              }}
              onMouseEnter={(e) => e.currentTarget.style.background = '#f8f8f8'}
              onMouseLeave={(e) => e.currentTarget.style.background = 'white'}
            >
              <Trash2 size={16} color="#dc3545" />
              Delete
            </button>
          </div>
        )}
      </div>

      <div style={{ fontSize: '64px', marginTop: '20px' }}>
        {category.icon || '📁'}
      </div>

      <h3 style={{
        margin: 0, fontSize: '20px', fontWeight: 'bold',
        color: '#333', textAlign: 'center'
      }}>
        {category.name}
      </h3>
    </div>
  );

  // Render subcategory card
  const renderSubcategoryCard = (subcategory) => (
    <div
      key={subcategory.id}
      style={{
        background: 'white',
        borderRadius: '16px',
        padding: '24px',
        boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
        transition: 'transform 0.2s, box-shadow 0.2s',
        cursor: 'pointer',
        position: 'relative',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '12px',
        minHeight: '180px'
      }}
      onClick={() => navigateToSubcategory(subcategory)}
      onMouseEnter={(e) => {
        e.currentTarget.style.transform = 'translateY(-4px)';
        e.currentTarget.style.boxShadow = '0 8px 24px rgba(255, 107, 53, 0.2)';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.transform = 'translateY(0)';
        e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.1)';
      }}
    >
      <div style={{ position: 'absolute', top: '12px', right: '12px' }}>
        <button
          onClick={(e) => {
            e.stopPropagation();
            setOpenDropdown(openDropdown === `subcategory-${subcategory.id}` ? null : `subcategory-${subcategory.id}`);
          }}
          style={{
            padding: '6px', background: 'transparent', border: 'none',
            borderRadius: '8px', color: '#666', cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            transition: 'all 0.2s'
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.background = '#f0f0f0';
            e.currentTarget.style.color = '#333';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.background = 'transparent';
            e.currentTarget.style.color = '#666';
          }}
        >
          <MoreVertical size={20} />
        </button>

        {openDropdown === `subcategory-${subcategory.id}` && (
          <div
            ref={dropdownRef}
            style={{
              position: 'absolute', top: '36px', right: '0',
              background: 'white', borderRadius: '12px',
              boxShadow: '0 4px 16px rgba(0,0,0,0.15)',
              minWidth: '140px', zIndex: 1000
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <button
              onClick={(e) => {
                e.stopPropagation();
                openEditSubcategoryModal(subcategory);
                setOpenDropdown(null);
              }}
              style={{
                width: '100%', padding: '12px 16px',
                background: 'white', border: 'none', cursor: 'pointer',
                display: 'flex', alignItems: 'center', gap: '10px',
                fontSize: '14px', fontWeight: '500', color: '#333',
                transition: 'background 0.2s'
              }}
              onMouseEnter={(e) => e.currentTarget.style.background = '#f8f8f8'}
              onMouseLeave={(e) => e.currentTarget.style.background = 'white'}
            >
              <Edit2 size={16} color="#4CAF50" />
              Edit
            </button>
            <button
              onClick={(e) => {
                e.stopPropagation();
                handleDeleteSubcategory(subcategory.id);
                setOpenDropdown(null);
              }}
              style={{
                width: '100%', padding: '12px 16px',
                background: 'white', border: 'none', cursor: 'pointer',
                display: 'flex', alignItems: 'center', gap: '10px',
                fontSize: '14px', fontWeight: '500', color: '#333',
                transition: 'background 0.2s'
              }}
              onMouseEnter={(e) => e.currentTarget.style.background = '#f8f8f8'}
              onMouseLeave={(e) => e.currentTarget.style.background = 'white'}
            >
              <Trash2 size={16} color="#dc3545" />
              Delete
            </button>
          </div>
        )}
      </div>

      <div style={{ fontSize: '64px', marginTop: '20px' }}>
        {subcategory.icon || '📂'}
      </div>

      <h3 style={{
        margin: 0, fontSize: '20px', fontWeight: 'bold',
        color: '#333', textAlign: 'center'
      }}>
        {subcategory.name}
      </h3>
    </div>
  );

  // Render Add Habit Modal
  const renderHabitModal = () => {
    const isEdit = !!editingHabit;
    const current = isEdit ? editingHabit : newHabit;
    const setCurrent = isEdit ? setEditingHabit : setNewHabit;

    const handleClose = () => {
      if (isEdit) {
        setEditingHabit(null);
      } else {
        setShowHabitModal(false);
        resetHabitForm();
      }
      setShowEmojiPicker(false);
      setAvailableSubcategories([]);
    };

    return (
      <div
        style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.5)', display: 'flex',
          alignItems: 'center', justifyContent: 'center',
          zIndex: 10000, padding: '20px'
        }}
        onClick={(e) => e.target === e.currentTarget && handleClose()}
      >
        <div style={{
          background: 'white', borderRadius: '16px', padding: '32px',
          maxWidth: '600px', width: '100%', maxHeight: '90vh',
          overflow: 'auto', position: 'relative'
        }}
        onClick={(e) => e.stopPropagation()}>
          <button
            onClick={handleClose}
            style={{
              position: 'absolute', top: '16px', right: '16px',
              background: 'none', border: 'none', cursor: 'pointer',
              padding: '8px', display: 'flex', alignItems: 'center',
              justifyContent: 'center', borderRadius: '50%',
              transition: 'background 0.2s'
            }}
            onMouseEnter={(e) => e.currentTarget.style.background = '#f0f0f0'}
            onMouseLeave={(e) => e.currentTarget.style.background = 'none'}
          >
            <X size={24} color="#666" />
          </button>

          <h2 style={{ margin: '0 0 24px 0', color: '#ff6b35', fontSize: '28px' }}>
            {isEdit ? 'Edit Habit' : 'Add New Habit'}
          </h2>

          <div style={{ marginBottom: '20px' }}>
            <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
              Habit name *
            </label>
            <input
              type="text"
              value={current.name}
              onChange={(e) => setCurrent({ ...current, name: e.target.value })}
              placeholder="e.g. Go to the gym, Meditate, Read"
              style={{
                width: '100%', padding: '12px 16px', fontSize: '16px',
                borderRadius: '12px', border: '2px solid #e0e0e0',
                outline: 'none', transition: 'border-color 0.2s',
                boxSizing: 'border-box'
              }}
              onFocus={(e) => e.target.style.borderColor = '#ff6b35'}
              onBlur={(e) => e.target.style.borderColor = '#e0e0e0'}
            />
          </div>

          <div style={{ marginBottom: '20px' }}>
            <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
              Description
            </label>
            <textarea
              value={current.description}
              onChange={(e) => setCurrent({ ...current, description: e.target.value })}
              placeholder="Optional description..."
              rows={3}
              style={{
                width: '100%', padding: '12px 16px', fontSize: '16px',
                borderRadius: '12px', border: '2px solid #e0e0e0',
                outline: 'none', transition: 'border-color 0.2s',
                resize: 'vertical', fontFamily: 'inherit',
                boxSizing: 'border-box'
              }}
              onFocus={(e) => e.target.style.borderColor = '#ff6b35'}
              onBlur={(e) => e.target.style.borderColor = '#e0e0e0'}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '20px' }}>
            <div>
              <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
                Category *
              </label>
              <select
                value={current.categoryId || ''}
                onChange={(e) => {
                  const catId = e.target.value ? Number(e.target.value) : null;
                  setCurrent({ ...current, categoryId: catId, subcategoryId: null });
                }}
                style={{
                  width: '100%', padding: '12px 16px', fontSize: '16px',
                  borderRadius: '12px', border: '2px solid #e0e0e0',
                  outline: 'none', transition: 'border-color 0.2s',
                  backgroundColor: 'white', boxSizing: 'border-box'
                }}
                onFocus={(e) => e.target.style.borderColor = '#ff6b35'}
                onBlur={(e) => e.target.style.borderColor = '#e0e0e0'}
              >
                <option value="">Select category</option>
                {categoriesWithSubcategories.map(cat => (
                  <option key={cat.id} value={cat.id}>
                    {cat.icon} {cat.name}
                  </option>
                ))}
              </select>
              {categoriesWithSubcategories.length === 0 && (
                <div style={{
                  fontSize: '13px', color: '#ff6b35', marginTop: '6px',
                  display: 'flex', alignItems: 'center', gap: '8px',
                  padding: '8px 12px', background: '#fff5eb',
                  borderRadius: '8px', border: '1px solid #ffd6b8'
                }}>
                  <span style={{ fontSize: '16px' }}>⚠️</span>
                  <span>
                    No categories with subcategories available.
                    <button
                      onClick={(e) => {
                        e.preventDefault();
                        handleClose();
                        setCurrentView('categories');
                      }}
                      style={{
                        marginLeft: '4px', color: '#ff6b35',
                        textDecoration: 'underline', background: 'none',
                        border: 'none', cursor: 'pointer',
                        fontSize: '13px', fontWeight: '600', padding: 0
                      }}
                    >
                      Set up now →
                    </button>
                  </span>
                </div>
              )}
            </div>

            <div>
              <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
                Subcategory *
              </label>
              <select
                value={current.subcategoryId || ''}
                onChange={(e) => {
                  const subId = e.target.value ? Number(e.target.value) : null;
                  setCurrent({ ...current, subcategoryId: subId });
                }}
                disabled={!current.categoryId || availableSubcategories.length === 0}
                style={{
                  width: '100%', padding: '12px 16px', fontSize: '16px',
                  borderRadius: '12px', border: '2px solid #e0e0e0',
                  outline: 'none', transition: 'border-color 0.2s',
                  backgroundColor: 'white',
                  opacity: !current.categoryId || availableSubcategories.length === 0 ? 0.5 : 1,
                  cursor: !current.categoryId || availableSubcategories.length === 0 ? 'not-allowed' : 'pointer',
                  boxSizing: 'border-box'
                }}
                onFocus={(e) => e.target.style.borderColor = '#ff6b35'}
                onBlur={(e) => e.target.style.borderColor = '#e0e0e0'}
              >
                <option value="">Select subcategory</option>
                {availableSubcategories.map(sub => (
                  <option key={sub.id} value={sub.id}>
                    {sub.icon} {sub.name}
                  </option>
                ))}
              </select>
              {!current.categoryId && (
                <div style={{ fontSize: '12px', color: '#999', marginTop: '4px' }}>
                  Select a category first
                </div>
              )}
              {current.categoryId && availableSubcategories.length === 0 && (
                <div style={{
                  fontSize: '13px', color: '#ff6b35', marginTop: '6px',
                  display: 'flex', alignItems: 'center', gap: '8px',
                  padding: '8px 12px', background: '#fff5eb',
                  borderRadius: '8px', border: '1px solid #ffd6b8'
                }}>
                  <span style={{ fontSize: '16px' }}>⚠️</span>
                  <span>
                    This category has no subcategories. Please select a different category or
                    <button
                      onClick={(e) => {
                        e.preventDefault();
                        handleClose();
                        setCurrentView('categories');
                      }}
                      style={{
                        marginLeft: '4px', color: '#ff6b35',
                        textDecoration: 'underline', background: 'none',
                        border: 'none', cursor: 'pointer',
                        fontSize: '13px', fontWeight: '600', padding: 0
                      }}
                    >
                      add subcategories →
                    </button>
                  </span>
                </div>
              )}
            </div>
          </div>

          <div style={{ marginBottom: '20px' }}>
            <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
              Duration (days)
            </label>
            <input
              type="number"
              value={current.durationDays}
              onChange={(e) => setCurrent({ ...current, durationDays: parseInt(e.target.value) || 1 })}
              min="1"
              max="365"
              style={{
                width: '100%', padding: '12px 16px', fontSize: '16px',
                borderRadius: '12px', border: '2px solid #e0e0e0',
                outline: 'none', transition: 'border-color 0.2s',
                boxSizing: 'border-box'
              }}
              onFocus={(e) => e.target.style.borderColor = '#ff6b35'}
              onBlur={(e) => e.target.style.borderColor = '#e0e0e0'}
            />
          </div>

          <div style={{ marginBottom: '24px' }}>
            <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
              Choose an icon
            </label>
            <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
              <div style={{
                width: '60px', height: '60px', borderRadius: '12px',
                border: '2px solid #e0e0e0', display: 'flex',
                alignItems: 'center', justifyContent: 'center',
                fontSize: '36px', background: '#f8f8f8', flexShrink: 0
              }}>
                {current.icon}
              </div>
              <button
                type="button"
                onClick={() => setShowEmojiPicker(!showEmojiPicker)}
                style={{
                  flex: 1, padding: '12px 16px',
                  background: showEmojiPicker ? '#fff5eb' : '#f8f8f8',
                  border: `2px solid ${showEmojiPicker ? '#ff6b35' : '#e0e0e0'}`,
                  borderRadius: '12px', cursor: 'pointer',
                  display: 'flex', alignItems: 'center',
                  justifyContent: 'center', gap: '8px',
                  fontWeight: '600',
                  color: showEmojiPicker ? '#ff6b35' : '#666',
                  fontSize: '16px', transition: 'all 0.2s'
                }}
                onMouseEnter={(e) => {
                  if (!showEmojiPicker) {
                    e.currentTarget.style.borderColor = '#ff6b35';
                    e.currentTarget.style.color = '#ff6b35';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!showEmojiPicker) {
                    e.currentTarget.style.borderColor = '#e0e0e0';
                    e.currentTarget.style.color = '#666';
                  }
                }}
              >
                <Smile size={20} />
                {showEmojiPicker ? 'Close Picker' : 'Choose Icon'}
              </button>
            </div>
          </div>

          {showEmojiPicker && (
            <div style={{
              marginBottom: '24px', border: '2px solid #ff6b35',
              borderRadius: '12px', overflow: 'hidden',
              boxShadow: '0 4px 12px rgba(255, 107, 53, 0.2)'
            }}>
              <EmojiPicker
                onEmojiClick={handleEmojiClick}
                width="100%"
                height={320}
                searchPlaceHolder="Search emoji..."
                previewConfig={{ showPreview: false }}
              />
            </div>
          )}

          <div style={{ display: 'flex', gap: '12px', marginTop: '32px' }}>
            <button
              onClick={handleClose}
              style={{
                flex: 1, padding: '14px', background: '#f0f0f0',
                border: 'none', borderRadius: '12px',
                fontSize: '16px', fontWeight: 'bold', color: '#666',
                cursor: 'pointer', transition: 'background 0.2s'
              }}
              onMouseEnter={(e) => e.currentTarget.style.background = '#e0e0e0'}
              onMouseLeave={(e) => e.currentTarget.style.background = '#f0f0f0'}
            >
              Cancel
            </button>
            <button
              onClick={isEdit ? handleEditHabit : handleCreateHabit}
              disabled={!current.name.trim() || !current.categoryId || !current.subcategoryId}
              style={{
                flex: 1, padding: '14px',
                background: (!current.name.trim() || !current.categoryId || !current.subcategoryId) ?
                  '#e0e0e0' :
                  isEdit ?
                    'linear-gradient(135deg, #4CAF50 0%, #45a049 100%)' :
                    'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
                border: 'none', borderRadius: '12px',
                fontSize: '16px', fontWeight: 'bold',
                color: (!current.name.trim() || !current.categoryId || !current.subcategoryId) ? '#999' : 'white',
                cursor: (!current.name.trim() || !current.categoryId || !current.subcategoryId) ? 'not-allowed' : 'pointer',
                boxShadow: (!current.name.trim() || !current.categoryId || !current.subcategoryId) ?
                  'none' :
                  isEdit ?
                    '0 4px 12px rgba(76, 175, 80, 0.3)' :
                    '0 4px 12px rgba(255, 107, 53, 0.3)',
                transition: 'transform 0.2s, box-shadow 0.2s',
                opacity: (!current.name.trim() || !current.categoryId || !current.subcategoryId) ? 0.6 : 1
              }}
              onMouseEnter={(e) => {
                if (current.name.trim() && current.categoryId && current.subcategoryId) {
                  e.currentTarget.style.transform = 'translateY(-2px)';
                  e.currentTarget.style.boxShadow = isEdit ?
                    '0 6px 16px rgba(76, 175, 80, 0.4)' :
                    '0 6px 16px rgba(255, 107, 53, 0.4)';
                }
              }}
              onMouseLeave={(e) => {
                if (current.name.trim() && current.categoryId && current.subcategoryId) {
                  e.currentTarget.style.transform = 'translateY(0)';
                  e.currentTarget.style.boxShadow = isEdit ?
                    '0 4px 12px rgba(76, 175, 80, 0.3)' :
                    '0 4px 12px rgba(255, 107, 53, 0.3)';
                }
              }}
            >
              {isEdit ? 'Save Changes' : 'Create Habit'}
            </button>
          </div>
        </div>
      </div>
    );
  };

  // Render modal for category/subcategory
  const renderModal = (type) => {
    const isCategory = type === 'createCategory' || type === 'editCategory';
    const isEdit = type === 'editCategory' || type === 'editSubcategory';

    const current = isCategory ?
      (isEdit ? editingCategory : newCategory) :
      (isEdit ? editingSubcategory : newSubcategory);

    const setCurrent = isCategory ?
      (isEdit ? setEditingCategory : setNewCategory) :
      (isEdit ? setEditingSubcategory : setNewSubcategory);

    const title = isCategory ?
      (isEdit ? 'Edit Category' : 'Create Category') :
      (isEdit ? 'Edit Subcategory' : 'Create Subcategory');

    const handleSubmit = isCategory ?
      (isEdit ? handleEditCategory : handleCreateCategory) :
      (isEdit ? handleEditSubcategory : handleCreateSubcategory);

    const handleClose = () => {
      if (isEdit) {
        isCategory ? setEditingCategory(null) : setEditingSubcategory(null);
      } else {
        isCategory ? setShowCategoryModal(false) : setShowSubcategoryModal(false);
        isCategory ? resetCategoryForm() : resetSubcategoryForm();
      }
      setShowEmojiPicker(false);
    };

    return (
      <div
        style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.5)', display: 'flex',
          alignItems: 'center', justifyContent: 'center',
          zIndex: 10000, padding: '20px'
        }}
        onClick={(e) => e.target === e.currentTarget && handleClose()}
      >
        <div style={{
          background: 'white', borderRadius: '16px', padding: '32px',
          maxWidth: '500px', width: '100%', maxHeight: '90vh',
          overflow: 'auto', position: 'relative'
        }}
        onClick={(e) => e.stopPropagation()}>
          <button
            onClick={handleClose}
            style={{
              position: 'absolute', top: '16px', right: '16px',
              background: 'none', border: 'none', cursor: 'pointer',
              padding: '8px', display: 'flex', alignItems: 'center',
              justifyContent: 'center', borderRadius: '50%',
              transition: 'background 0.2s'
            }}
            onMouseEnter={(e) => e.currentTarget.style.background = '#f0f0f0'}
            onMouseLeave={(e) => e.currentTarget.style.background = 'none'}
          >
            <X size={24} color="#666" />
          </button>

          <h2 style={{ margin: '0 0 24px 0', color: '#ff6b35', fontSize: '28px' }}>
            {title}
          </h2>

          <div style={{ marginBottom: '24px' }}>
            <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
              {isCategory ? 'Category' : 'Subcategory'} name
            </label>
            <input
              type="text"
              value={current.name}
              onChange={(e) => setCurrent({ ...current, name: e.target.value })}
              placeholder={isCategory ? 'e.g. Health, Productivity' : 'e.g. Gym, Meditation'}
              style={{
                width: '100%', padding: '12px 16px', fontSize: '16px',
                borderRadius: '12px', border: '2px solid #e0e0e0',
                outline: 'none', transition: 'border-color 0.2s',
                boxSizing: 'border-box'
              }}
              onFocus={(e) => e.target.style.borderColor = '#ff6b35'}
              onBlur={(e) => e.target.style.borderColor = '#e0e0e0'}
            />
          </div>

          <div style={{ marginBottom: '24px' }}>
            <label style={{ display: 'block', marginBottom: '8px', fontWeight: '600', color: '#333' }}>
              Choose an icon
            </label>
            <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
              <div style={{
                width: '60px', height: '60px', borderRadius: '12px',
                border: '2px solid #e0e0e0', display: 'flex',
                alignItems: 'center', justifyContent: 'center',
                fontSize: '36px', background: '#f8f8f8', flexShrink: 0
              }}>
                {current.icon}
              </div>
              <button
                type="button"
                onClick={() => setShowEmojiPicker(!showEmojiPicker)}
                style={{
                  flex: 1, padding: '12px 16px',
                  background: showEmojiPicker ? '#fff5eb' : '#f8f8f8',
                  border: `2px solid ${showEmojiPicker ? '#ff6b35' : '#e0e0e0'}`,
                  borderRadius: '12px', cursor: 'pointer',
                  display: 'flex', alignItems: 'center',
                  justifyContent: 'center', gap: '8px',
                  fontWeight: '600',
                  color: showEmojiPicker ? '#ff6b35' : '#666',
                  fontSize: '16px', transition: 'all 0.2s'
                }}
                onMouseEnter={(e) => {
                  if (!showEmojiPicker) {
                    e.currentTarget.style.borderColor = '#ff6b35';
                    e.currentTarget.style.color = '#ff6b35';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!showEmojiPicker) {
                    e.currentTarget.style.borderColor = '#e0e0e0';
                    e.currentTarget.style.color = '#666';
                  }
                }}
              >
                <Smile size={20} />
                {showEmojiPicker ? 'Close Picker' : (isEdit ? 'Change Icon' : 'Choose Icon')}
              </button>
            </div>
          </div>

          {showEmojiPicker && (
            <div style={{
              marginBottom: '24px', border: '2px solid #ff6b35',
              borderRadius: '12px', overflow: 'hidden',
              boxShadow: '0 4px 12px rgba(255, 107, 53, 0.2)'
            }}>
              <EmojiPicker
                onEmojiClick={handleEmojiClick}
                width="100%"
                height={320}
                searchPlaceHolder="Search emoji..."
                previewConfig={{ showPreview: false }}
              />
            </div>
          )}

          <div style={{ display: 'flex', gap: '12px', marginTop: '32px' }}>
            <button
              onClick={handleClose}
              style={{
                flex: 1, padding: '14px', background: '#f0f0f0',
                border: 'none', borderRadius: '12px',
                fontSize: '16px', fontWeight: 'bold', color: '#666',
                cursor: 'pointer', transition: 'background 0.2s'
              }}
              onMouseEnter={(e) => e.currentTarget.style.background = '#e0e0e0'}
              onMouseLeave={(e) => e.currentTarget.style.background = '#f0f0f0'}
            >
              Cancel
            </button>
            <button
              onClick={handleSubmit}
              style={{
                flex: 1, padding: '14px',
                background: isEdit ?
                  'linear-gradient(135deg, #4CAF50 0%, #45a049 100%)' :
                  'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
                border: 'none', borderRadius: '12px',
                fontSize: '16px', fontWeight: 'bold', color: 'white',
                cursor: 'pointer',
                boxShadow: isEdit ?
                  '0 4px 12px rgba(76, 175, 80, 0.3)' :
                  '0 4px 12px rgba(255, 107, 53, 0.3)',
                transition: 'transform 0.2s, box-shadow 0.2s'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'translateY(-2px)';
                e.currentTarget.style.boxShadow = isEdit ?
                  '0 6px 16px rgba(76, 175, 80, 0.4)' :
                  '0 6px 16px rgba(255, 107, 53, 0.4)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = isEdit ?
                  '0 4px 12px rgba(76, 175, 80, 0.3)' :
                  '0 4px 12px rgba(255, 107, 53, 0.3)';
              }}
            >
              {isEdit ? 'Save Changes' : (isCategory ? 'Create Category' : 'Create Subcategory')}
            </button>
          </div>
        </div>
      </div>
    );
  };

  return (
    <div style={{ padding: '80px 40px', maxWidth: '1400px', margin: '0 auto' }}>
      {/* CSS Animation */}
      <style>{`
        @keyframes slideIn {
          from {
            transform: translateX(400px);
            opacity: 0;
          }
          to {
            transform: translateX(0);
            opacity: 1;
          }
        }
      `}</style>

      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '32px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          {currentView !== 'habits' && (
            <button
              onClick={navigateBack}
              style={{
                padding: '10px', background: 'white',
                border: '2px solid #ff6b35', borderRadius: '10px',
                color: '#ff6b35', cursor: 'pointer',
                display: 'flex', alignItems: 'center',
                justifyContent: 'center', transition: 'all 0.2s'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.background = '#ff6b35';
                e.currentTarget.style.color = 'white';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = 'white';
                e.currentTarget.style.color = '#ff6b35';
              }}
            >
              <ArrowLeft size={20} />
            </button>
          )}
          <Target size={32} color="#ff6b35" />
          <h1 style={{ margin: 0, color: '#ff6b35', fontSize: '36px' }}>
            {currentView === 'habits' ? 'My Habits' :
             currentView === 'categories' ? 'Manage Categories' :
             currentView === 'subcategories' ? selectedCategory?.name :
             selectedSubcategory?.name}
          </h1>
        </div>

        {/* Action buttons based on current view */}
        <div style={{ display: 'flex', gap: '12px' }}>
          {currentView === 'habits' && (
            <>
              {canAddHabit ? (
                <button
                  onClick={() => setShowHabitModal(true)}
                  style={{
                    display: 'flex', alignItems: 'center', gap: '8px',
                    padding: '12px 24px',
                    background: 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
                    color: 'white', border: 'none', borderRadius: '12px',
                    fontSize: '16px', fontWeight: 'bold', cursor: 'pointer',
                    boxShadow: '0 4px 12px rgba(255, 107, 53, 0.3)',
                    transition: 'transform 0.2s, box-shadow 0.2s'
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.transform = 'translateY(-2px)';
                    e.currentTarget.style.boxShadow = '0 6px 16px rgba(255, 107, 53, 0.4)';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.transform = 'translateY(0)';
                    e.currentTarget.style.boxShadow = '0 4px 12px rgba(255, 107, 53, 0.3)';
                  }}
                >
                  <Plus size={20} />
                  Add Habit
                </button>
              ) : (
                <div style={{
                  display: 'flex', alignItems: 'center', gap: '8px',
                  padding: '12px 20px', background: '#fff5eb',
                  color: '#ff6b35', border: '2px solid #ffd6b8',
                  borderRadius: '12px', fontSize: '14px', fontWeight: '600'
                }}>
                  <span style={{ fontSize: '18px' }}>⚠️</span>
                  Set up categories first
                </div>
              )}
              <button
                onClick={() => setCurrentView('categories')}
                style={{
                  display: 'flex', alignItems: 'center', gap: '8px',
                  padding: '12px 24px', background: 'white',
                  color: '#ff6b35', border: '2px solid #ff6b35',
                  borderRadius: '12px', fontSize: '16px',
                  fontWeight: 'bold', cursor: 'pointer',
                  transition: 'all 0.2s'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = '#fff5eb';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = 'white';
                }}
              >
                <Layers size={20} />
                Manage Categories
              </button>
            </>
          )}

          {currentView === 'subcategoryHabits' && (
            <button
              onClick={() => {
                setNewHabit({
                  ...newHabit,
                  categoryId: selectedCategory.id,
                  subcategoryId: selectedSubcategory.id
                });
                setShowHabitModal(true);
              }}
              style={{
                display: 'flex', alignItems: 'center', gap: '8px',
                padding: '12px 24px',
                background: 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
                color: 'white', border: 'none', borderRadius: '12px',
                fontSize: '16px', fontWeight: 'bold', cursor: 'pointer',
                boxShadow: '0 4px 12px rgba(255, 107, 53, 0.3)',
                transition: 'transform 0.2s, box-shadow 0.2s'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'translateY(-2px)';
                e.currentTarget.style.boxShadow = '0 6px 16px rgba(255, 107, 53, 0.4)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = '0 4px 12px rgba(255, 107, 53, 0.3)';
              }}
            >
              <Plus size={20} />
              Add Habit
            </button>
          )}

          {currentView === 'categories' && (
            <button
              onClick={() => setShowCategoryModal(true)}
              style={{
                display: 'flex', alignItems: 'center', gap: '8px',
                padding: '12px 24px',
                background: 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
                color: 'white', border: 'none', borderRadius: '12px',
                fontSize: '16px', fontWeight: 'bold', cursor: 'pointer',
                boxShadow: '0 4px 12px rgba(255, 107, 53, 0.3)',
                transition: 'transform 0.2s, box-shadow 0.2s'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'translateY(-2px)';
                e.currentTarget.style.boxShadow = '0 6px 16px rgba(255, 107, 53, 0.4)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = '0 4px 12px rgba(255, 107, 53, 0.3)';
              }}
            >
              <Plus size={20} />
              Add Category
            </button>
          )}

          {currentView === 'subcategories' && (
            <button
              onClick={() => setShowSubcategoryModal(true)}
              style={{
                display: 'flex', alignItems: 'center', gap: '8px',
                padding: '12px 24px',
                background: 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
                color: 'white', border: 'none', borderRadius: '12px',
                fontSize: '16px', fontWeight: 'bold', cursor: 'pointer',
                boxShadow: '0 4px 12px rgba(255, 107, 53, 0.3)',
                transition: 'transform 0.2s, box-shadow 0.2s'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'translateY(-2px)';
                e.currentTarget.style.boxShadow = '0 6px 16px rgba(255, 107, 53, 0.4)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = '0 4px 12px rgba(255, 107, 53, 0.3)';
              }}
            >
              <Plus size={20} />
              Add Subcategory
            </button>
          )}
        </div>
      </div>

      {/* Breadcrumb */}
      {renderBreadcrumb()}

      {/* Loading/Error states */}
      {loading && <div style={{ textAlign: 'center', padding: '40px', color: '#666' }}>Loading...</div>}
      {error && <div style={{ textAlign: 'center', padding: '40px', color: '#dc3545' }}>{error}</div>}

      {/* Content based on current view */}
      {!loading && !error && (
        <>
          {/* Main habits view with table */}
          {currentView === 'habits' && (
            <>
              {habits.length === 0 ? (
                renderEmptyState('habits')
              ) : (
                <>
                  {/* Celebration Modal */}
                  {renderCelebrationModal()}

                  {/* In Progress Habits */}
                  {inProgressHabits.length > 0 && (
                    <>
                      <h2 style={{
                        margin: '0 0 20px 0',
                        color: '#333',
                        fontSize: '24px',
                        fontWeight: 'bold',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '12px'
                      }}>
                        <Target size={28} color="#ff6b35" />
                        In Progress
                        <span style={{
                          background: '#ff6b35',
                          color: 'white',
                          padding: '4px 12px',
                          borderRadius: '20px',
                          fontSize: '16px',
                          fontWeight: 'bold'
                        }}>
                          {inProgressHabits.length}
                        </span>
                      </h2>
                      {renderHabitsTable(inProgressHabits, false)}
                    </>
                  )}

                  {/* Completed Habits */}
                  {completedHabits.length > 0 && (
                    <>
                      <h2 style={{
                        margin: '32px 0 20px 0',
                        color: '#333',
                        fontSize: '24px',
                        fontWeight: 'bold',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '12px'
                      }}>
                        <Trophy size={28} color="#FFD700" />
                        Completed
                        <span style={{
                          background: 'linear-gradient(135deg, #FFD700 0%, #FFA500 100%)',
                          color: 'white',
                          padding: '4px 12px',
                          borderRadius: '20px',
                          fontSize: '16px',
                          fontWeight: 'bold'
                        }}>
                          {completedHabits.length}
                        </span>
                      </h2>
                      {renderHabitsTable(completedHabits, true)}
                    </>
                  )}
                </>
              )}
            </>
          )}

          {/* Categories management view */}
          {currentView === 'categories' && (
            <>
              {categories.length === 0 ? (
                renderEmptyState('categories')
              ) : (
                <div style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
                  gap: '20px'
                }}>
                  {categories.map(renderCategoryCard)}
                </div>
              )}
            </>
          )}

          {/* Subcategories view */}
          {currentView === 'subcategories' && (
            <>
              {subcategories.length === 0 ? (
                renderEmptyState('subcategories')
              ) : (
                <div style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
                  gap: '20px'
                }}>
                  {subcategories.map(renderSubcategoryCard)}
                </div>
              )}
            </>
          )}

          {/* Subcategory habits view */}
          {currentView === 'subcategoryHabits' && (
            <>
              {(() => {
                const subcategoryHabits = habits.filter(
                  h => h.subcategoryId === selectedSubcategory?.id
                );

                if (subcategoryHabits.length === 0) {
                  return (
                    <div style={{
                      padding: '60px 40px',
                      textAlign: 'center',
                      background: 'white',
                      borderRadius: '16px',
                      boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
                    }}>
                      <Target size={64} color="#ff6b35" style={{ opacity: 0.3, marginBottom: '16px' }} />
                      <h2 style={{ color: '#333', marginBottom: '8px', fontSize: '24px' }}>
                        No habits here yet!
                      </h2>
                      <p style={{ margin: '0 0 24px 0', color: '#666' }}>
                        Create your first habit in "{selectedSubcategory?.name}"
                      </p>
                      <button
                        onClick={() => {
                          setNewHabit({
                            ...newHabit,
                            categoryId: selectedCategory.id,
                            subcategoryId: selectedSubcategory.id
                          });
                          setShowHabitModal(true);
                        }}
                        style={{
                          padding: '12px 32px',
                          background: 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
                          color: 'white',
                          border: 'none',
                          borderRadius: '12px',
                          fontSize: '16px',
                          fontWeight: 'bold',
                          cursor: 'pointer',
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '8px',
                          boxShadow: '0 4px 12px rgba(255, 107, 53, 0.3)',
                          transition: 'transform 0.2s, box-shadow 0.2s'
                        }}
                        onMouseEnter={(e) => {
                          e.currentTarget.style.transform = 'translateY(-2px)';
                          e.currentTarget.style.boxShadow = '0 6px 16px rgba(255, 107, 53, 0.4)';
                        }}
                        onMouseLeave={(e) => {
                          e.currentTarget.style.transform = 'translateY(0)';
                          e.currentTarget.style.boxShadow = '0 4px 12px rgba(255, 107, 53, 0.3)';
                        }}
                      >
                        <Plus size={20} />
                        Add Habit
                      </button>
                    </div>
                  );
                }

                return (
                  <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
                    gap: '20px'
                  }}>
                    {subcategoryHabits.map((habit) => (
                      <div
                        key={habit.id}
                        style={{
                          background: 'white',
                          borderRadius: '16px',
                          padding: '24px',
                          boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                          transition: 'transform 0.2s, box-shadow 0.2s',
                          position: 'relative'
                        }}
                        onMouseEnter={(e) => {
                          e.currentTarget.style.transform = 'translateY(-4px)';
                          e.currentTarget.style.boxShadow = '0 8px 24px rgba(255, 107, 53, 0.2)';
                        }}
                        onMouseLeave={(e) => {
                          e.currentTarget.style.transform = 'translateY(0)';
                          e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.1)';
                        }}
                      >
                        {/* Three-dot menu */}
                        <div style={{ position: 'absolute', top: '12px', right: '12px' }}>
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setOpenDropdown(openDropdown === `habit-card-${habit.id}` ? null : `habit-card-${habit.id}`);
                            }}
                            style={{
                              padding: '6px', background: 'transparent', border: 'none',
                              borderRadius: '8px', color: '#666', cursor: 'pointer',
                              display: 'flex', alignItems: 'center',
                              justifyContent: 'center', transition: 'all 0.2s'
                            }}
                            onMouseEnter={(e) => {
                              e.currentTarget.style.background = '#f0f0f0';
                              e.currentTarget.style.color = '#333';
                            }}
                            onMouseLeave={(e) => {
                              e.currentTarget.style.background = 'transparent';
                              e.currentTarget.style.color = '#666';
                            }}
                          >
                            <MoreVertical size={20} />
                          </button>

                          {/* Dropdown menu */}
                          {openDropdown === `habit-card-${habit.id}` && (
                            <div
                              ref={dropdownRef}
                              style={{
                                position: 'absolute', top: '36px', right: '0',
                                background: 'white', borderRadius: '12px',
                                boxShadow: '0 4px 16px rgba(0,0,0,0.15)',
                                minWidth: '140px', zIndex: 1000
                              }}
                              onClick={(e) => e.stopPropagation()}
                            >
                              {!habit.done && (
                                <button
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    openEditHabitModal(habit);
                                    setOpenDropdown(null);
                                  }}
                                  style={{
                                    width: '100%', padding: '12px 16px',
                                    background: 'white', border: 'none', cursor: 'pointer',
                                    display: 'flex', alignItems: 'center', gap: '10px',
                                    fontSize: '14px', fontWeight: '500', color: '#333',
                                    transition: 'background 0.2s'
                                  }}
                                  onMouseEnter={(e) => e.currentTarget.style.background = '#f8f8f8'}
                                  onMouseLeave={(e) => e.currentTarget.style.background = 'white'}
                                >
                                  <Edit2 size={16} color="#4CAF50" />
                                  Edit
                                </button>
                              )}
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleDeleteHabit(habit.id);
                                  setOpenDropdown(null);
                                }}
                                style={{
                                  width: '100%', padding: '12px 16px',
                                  background: 'white', border: 'none', cursor: 'pointer',
                                  display: 'flex', alignItems: 'center', gap: '10px',
                                  fontSize: '14px', fontWeight: '500', color: '#333',
                                  transition: 'background 0.2s'
                                }}
                                onMouseEnter={(e) => e.currentTarget.style.background = '#f8f8f8'}
                                onMouseLeave={(e) => e.currentTarget.style.background = 'white'}
                              >
                                <Trash2 size={16} color="#dc3545" />
                                Delete
                              </button>
                            </div>
                          )}
                        </div>

                        {/* Habit icon and info */}
                        <div style={{
                          display: 'flex',
                          flexDirection: 'column',
                          alignItems: 'center',
                          gap: '16px',
                          marginTop: '8px'
                        }}>
                          <div style={{ fontSize: '64px' }}>
                            {habit.icon || '🎯'}
                          </div>

                          <div style={{ textAlign: 'center', width: '100%' }}>
                            <div style={{
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              gap: '8px',
                              marginBottom: '8px'
                            }}>
                              <h3 style={{
                                margin: 0,
                                fontSize: '20px',
                                fontWeight: 'bold',
                                color: '#333'
                              }}>
                                {habit.name}
                              </h3>
                              {habit.description && (
                                <div
                                  className="habit-info-wrapper"
                                  style={{ position: 'relative', display: 'inline-block' }}
                                  onMouseEnter={(e) => {
                                    const tooltip = e.currentTarget.querySelector('.habit-tooltip');
                                    if (tooltip) tooltip.style.opacity = '1';
                                  }}
                                  onMouseLeave={(e) => {
                                    const tooltip = e.currentTarget.querySelector('.habit-tooltip');
                                    if (tooltip) tooltip.style.opacity = '0';
                                  }}
                                >
                                  <Info
                                    size={20}
                                    style={{
                                      color: '#ff6b35',
                                      cursor: 'pointer',
                                      flexShrink: 0
                                    }}
                                  />
                                  <div
                                    className="habit-tooltip"
                                    style={{
                                      position: 'absolute', bottom: '100%', left: '50%',
                                      transform: 'translateX(-50%)', marginBottom: '8px',
                                      padding: '8px 12px', background: '#333', color: 'white',
                                      borderRadius: '8px', fontSize: '13px', maxWidth: '250px',
                                      whiteSpace: 'normal', opacity: 0, pointerEvents: 'none',
                                      transition: 'opacity 0.2s', zIndex: 10000,
                                      boxShadow: '0 4px 12px rgba(0,0,0,0.15)', textAlign: 'left'
                                    }}
                                  >
                                    {habit.description}
                                    <div style={{
                                      position: 'absolute', top: '100%', left: '50%',
                                      transform: 'translateX(-50%)', width: 0, height: 0,
                                      borderLeft: '6px solid transparent', borderRight: '6px solid transparent',
                                      borderTop: '6px solid #333'
                                    }} />
                                  </div>
                                </div>
                              )}
                            </div>

                            {/* Streak badge */}
                            <div style={{
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '6px',
                              padding: '8px 16px',
                              background: habit.currentStreak > 0 ? '#fff5eb' : '#f8f8f8',
                              borderRadius: '20px',
                              fontSize: '16px',
                              fontWeight: '600',
                              color: habit.currentStreak > 0 ? '#ff6b35' : '#999',
                              marginBottom: '16px'
                            }}>
                              {habit.currentStreak > 0 && <Flame size={18} />}
                              {habit.currentStreak} {habit.currentStreak === 1 ? 'day' : 'days'} streak
                            </div>

                            {/* Mark done button */}
                            {!habit.done && (
                              <button
                                onClick={() => handleMarkDone(habit.id)}
                                disabled={markedDoneToday.has(habit.id)}
                                style={{
                                  width: '100%',
                                  padding: '12px 20px',
                                  background: markedDoneToday.has(habit.id) ?
                                    '#e0e0e0' :
                                    'linear-gradient(135deg, #4CAF50 0%, #45a049 100%)',
                                  color: markedDoneToday.has(habit.id) ? '#999' : 'white',
                                  border: 'none',
                                  borderRadius: '12px',
                                  fontSize: '16px',
                                  fontWeight: '600',
                                  cursor: markedDoneToday.has(habit.id) ? 'not-allowed' : 'pointer',
                                  display: 'flex',
                                  alignItems: 'center',
                                  justifyContent: 'center',
                                  gap: '8px',
                                  boxShadow: markedDoneToday.has(habit.id) ?
                                    'none' :
                                    '0 4px 12px rgba(76, 175, 80, 0.3)',
                                  transition: 'transform 0.2s, box-shadow 0.2s',
                                  opacity: markedDoneToday.has(habit.id) ? 0.6 : 1
                                }}
                                onMouseEnter={(e) => {
                                  if (!markedDoneToday.has(habit.id)) {
                                    e.currentTarget.style.transform = 'translateY(-2px)';
                                    e.currentTarget.style.boxShadow = '0 6px 16px rgba(76, 175, 80, 0.4)';
                                  }
                                }}
                                onMouseLeave={(e) => {
                                  if (!markedDoneToday.has(habit.id)) {
                                    e.currentTarget.style.transform = 'translateY(0)';
                                    e.currentTarget.style.boxShadow = '0 4px 12px rgba(76, 175, 80, 0.3)';
                                  }
                                }}
                              >
                                <CheckCircle size={18} />
                                {markedDoneToday.has(habit.id) ? 'Done!' : 'Mark Done'}
                              </button>
                            )}

                            {/* Completed badge */}
                            {habit.done && (
                              <div style={{
                                width: '100%',
                                padding: '10px 16px',
                                background: 'linear-gradient(135deg, #FFD700 0%, #FFA500 100%)',
                                color: 'white',
                                border: 'none',
                                borderRadius: '12px',
                                fontSize: '15px',
                                fontWeight: '600',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                gap: '6px',
                                boxSizing: 'border-box'
                              }}>
                                <Trophy size={16} />
                                Completed!
                              </div>
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                );
              })()}
            </>
          )}
        </>
      )}

      {/* Modals */}
      {showCategoryModal && renderModal('createCategory')}
      {editingCategory && renderModal('editCategory')}
      {showSubcategoryModal && renderModal('createSubcategory')}
      {editingSubcategory && renderModal('editSubcategory')}
      {(showHabitModal || editingHabit) && renderHabitModal()}
    </div>
  );
};

export default Habits;
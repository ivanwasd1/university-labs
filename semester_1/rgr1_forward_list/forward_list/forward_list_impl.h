#pragma once

#include <cstdint>
#include <cstddef>
#include <initializer_list>
#include <iterator>
#include <iostream>
#include <stdexcept>
#include <utility>

class ForwardList {
private:
    struct Node {
        int32_t value_;
        Node* next_;

        explicit Node(int32_t value) : value_(value), next_(nullptr) {}
    };

public:
    class ForwardListIterator {
    public:
        using iterator_category = std::forward_iterator_tag;
        using difference_type = std::ptrdiff_t;
        using value_type = int32_t;
        using pointer = value_type*;
        using reference = value_type&;

        explicit ForwardListIterator(Node* position) : position_(position) {}

        ForwardListIterator& operator++() {
            if (position_) position_ = position_->next_;
            return *this;
        }

        ForwardListIterator operator++(int) {
            ForwardListIterator tmp = *this;
            ++(*this);
            return tmp;
        }

        bool operator==(const ForwardListIterator& other) const {
            return position_ == other.position_;
        }

        bool operator!=(const ForwardListIterator& other) const {
            return position_ != other.position_;
        }

        reference operator*() const {
            return position_->value_;
        }

        pointer operator->() const {
            return &position_->value_;
        }

    private:
        Node* position_;
    };

    ForwardListIterator begin() { return ForwardListIterator(head_); }
    ForwardListIterator end() { return ForwardListIterator(nullptr); }

    ForwardListIterator begin() const { return ForwardListIterator(head_); }
    ForwardListIterator end() const { return ForwardListIterator(nullptr); }

    ForwardList();
    ForwardList(const ForwardList& rhs);
    ForwardList(size_t count, int32_t value);
    ForwardList(std::initializer_list<int32_t> init);

    ForwardList& operator=(const ForwardList& rhs);
    ~ForwardList();

    void PushFront(int32_t value);
    void PopFront();
    void Remove(int32_t value);
    void Clear();

    bool FindByValue(int32_t value) const;
    void Print(std::ostream& out) const;

    int32_t Front() const;
    size_t Size() const;

private:
    Node* head_;
    size_t size_;

    void Swap(ForwardList& other) noexcept {
        std::swap(head_, other.head_);
        std::swap(size_, other.size_);
    }
};

package com.printmomentum.web;

import java.util.List;

public record NichePageResponse(List<NicheTermItem> items, int page, int size, int total) {
}

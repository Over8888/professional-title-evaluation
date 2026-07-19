package com.ruoyi.evaluation.support;

import com.ruoyi.common.utils.StringUtils;

public final class EvaluationTitleUtils
{
    public static final String LEVEL_SUB_SENIOR_ENGINEER = "副高级工程师";
    public static final String LEVEL_INTERMEDIATE_ENGINEER = "中级工程师";
    public static final String LEVEL_ASSISTANT_ENGINEER = "助理工程师";
    public static final String LEVEL_TECHNICIAN = "技术员";

    private EvaluationTitleUtils()
    {
    }

    public static String normalizeAppliedLevel(String value)
    {
        String text = normalize(value);
        if (StringUtils.isEmpty(text))
        {
            return null;
        }
        if (containsAny(text, "正高级工程师", "正高级"))
        {
            return null;
        }
        if (containsAny(text, "副高级工程师", "高级工程师", "副高级", "副高", "高级"))
        {
            return LEVEL_SUB_SENIOR_ENGINEER;
        }
        if (containsAny(text, "助理工程师", "助理级", "助理", "初级"))
        {
            return LEVEL_ASSISTANT_ENGINEER;
        }
        if (containsAny(text, "技术员", "员级"))
        {
            return LEVEL_TECHNICIAN;
        }
        if (containsAny(text, "中级工程师", "中级", "工程师"))
        {
            return LEVEL_INTERMEDIATE_ENGINEER;
        }
        return null;
    }

    public static boolean isVotingLevel(String level)
    {
        String normalized = normalizeAppliedLevel(level);
        return LEVEL_SUB_SENIOR_ENGINEER.equals(normalized) || LEVEL_INTERMEDIATE_ENGINEER.equals(normalized);
    }

    public static boolean isAutoPassLevel(String level)
    {
        String normalized = normalizeAppliedLevel(level);
        return LEVEL_ASSISTANT_ENGINEER.equals(normalized) || LEVEL_TECHNICIAN.equals(normalized);
    }

    public static int levelRank(String level)
    {
        String normalized = normalizeAppliedLevel(level);
        if (LEVEL_SUB_SENIOR_ENGINEER.equals(normalized))
        {
            return 2;
        }
        if (LEVEL_INTERMEDIATE_ENGINEER.equals(normalized))
        {
            return 3;
        }
        if (LEVEL_ASSISTANT_ENGINEER.equals(normalized))
        {
            return 4;
        }
        if (LEVEL_TECHNICIAN.equals(normalized))
        {
            return 5;
        }
        return 99;
    }

    private static boolean containsAny(String text, String... needles)
    {
        for (String needle : needles)
        {
            if (text.contains(needle))
            {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.replace('\u3000', ' ').trim();
    }
}
